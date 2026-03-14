package io.github.dordor12;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Marker;


/**
 * A high-performance Logback appender that converts log events into Micrometer metrics.
 * <p>
 * Extracts key-value pairs from MDC properties, Logstash StructuredArguments,
 * and LogstashMarkers to create counters and histograms.
 * <p>
 * Designed for 50K+ logs/sec with minimal GC pressure:
 * <ul>
 *   <li>Zero-allocation hot path via CacheKey lookups</li>
 *   <li>No JSON round-tripping — extracts data directly from event objects</li>
 *   <li>Circuit breaker when metric limits are reached</li>
 *   <li>Fast numeric pre-check to avoid NumberFormatException stack traces</li>
 * </ul>
 * <p>
 * <b>Cardinality Protection</b>: When enabled, automatically detects tag keys
 * with too many distinct values (e.g., userId, requestId, traceId) and
 * blacklists them at runtime. Existing counters containing the blacklisted tag
 * are removed from the Micrometer registry and re-registered without the
 * offending tag, preventing unbounded series in metrics backends.
 * Histograms are excluded from cardinality protection.
 * <p>
 * <b>Self-Observability</b>: Registers internal metrics prefixed with
 * {@code logback.to.metrics.appender} for monitoring appender health:
 * <ul>
 *   <li>{@code append.duration} — Timer for time spent in append()</li>
 *   <li>{@code counters.created} — Counter for total counters registered</li>
 *   <li>{@code histograms.created} — Counter for total histograms registered</li>
 *   <li>{@code counters.active} — Gauge for current active counters</li>
 *   <li>{@code histograms.active} — Gauge for current active histograms</li>
 *   <li>{@code cardinality.blacklisted} — Counter for auto-blacklisted keys</li>
 *   <li>{@code cardinality.reregister.duration} — Timer for re-registration</li>
 *   <li>{@code counters.saturated} — Gauge: 1 if counter limit reached, else 0</li>
 *   <li>{@code events.dropped} — Counter for events skipped by circuit breaker</li>
 * </ul>
 */
@Getter
@Setter
public class LogbackToMetricsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final Set<String> FIXED_TAG_KEYS = Set.of("level", "logger_name", "thread_name");

    private List<String> kvWhitelist = new ArrayList<>();
    private List<String> kvBlacklist = new ArrayList<>();
    private Set<String> kvWhitelistSet = new HashSet<>();
    private Set<String> kvBlacklistSet = new HashSet<>();
    private ConcurrentHashMap<CacheKey, Counter> counters = new ConcurrentHashMap<>();
    private Long maxCounters = 10000L;
    private String counterJoinString = ".";
    private String counterNamePrefix = "logback.to.metrics";
    private String counterNameSubfix = "counter";

    // Histogram configuration
    private boolean enableAutoHistograms = false;
    private List<String> histogramKvWhitelist = new ArrayList<>();
    private List<String> histogramKvBlacklist = new ArrayList<>();
    private Set<String> histogramKvWhitelistSet = new HashSet<>();
    private Set<String> histogramKvBlacklistSet = new HashSet<>();
    private ConcurrentHashMap<CacheKey, DistributionSummary> histograms = new ConcurrentHashMap<>();
    private Long maxHistograms = 10000L;
    private String histogramNameSubfix = "histogram";

    // Cardinality protection (off by default)
    private boolean enableCardinalityProtection = false;
    private int maxTagValueCardinality = 100;

    // Per-tag-key tracking: key -> set of distinct values
    private final ConcurrentHashMap<String, Set<String>> cardinalityTracker = new ConcurrentHashMap<>();

    // Keys auto-blacklisted due to cardinality breach
    private final Set<String> autoBlacklistedKeys = ConcurrentHashMap.newKeySet();

    // Lock for re-registration (cold path only)
    private final ReentrantLock reRegisterLock = new ReentrantLock();

    // Circuit breaker flags — once saturated, skip all computation
    private volatile boolean countersSaturated = false;
    private volatile boolean histogramsSaturated = false;

    // Self-observability toggle (enabled by default)
    private boolean enableSelfObservability = true;

    // Self-observability metrics
    private Timer appendTimer;
    private Counter countersCreatedCounter;
    private Counter histogramsCreatedCounter;
    private Counter cardinalityBlacklistedCounter;
    private Timer reregisterTimer;
    private Counter eventsDroppedCounter;

    /**
     * Adds a key to the whitelist for metric tag extraction.
     * Only keys in the whitelist will be included as metric tags.
     * This method is called by Logback when parsing XML configuration.
     *
     * @param whiteList the key to add to the whitelist
     */
    public void addKvWhitelist(String whiteList) {
        this.kvWhitelist.add(whiteList);
        kvWhitelistSet.add(whiteList);
    }

    /**
     * Adds a key to the blacklist for metric tag extraction.
     * Keys in the blacklist will be excluded from metric tags.
     * This method is called by Logback when parsing XML configuration.
     *
     * @param blackList the key to add to the blacklist
     */
    public void addKvBlacklist(String blackList) {
        this.kvBlacklist.add(blackList);
        kvBlacklistSet.add(blackList);
    }

    /**
     * Adds a key to the whitelist for histogram creation.
     * Only keys in the whitelist will be considered for histogram creation.
     * This method is called by Logback when parsing XML configuration.
     *
     * @param whiteList the key to add to the histogram whitelist
     */
    public void addHistogramKvWhitelist(String whiteList) {
        this.histogramKvWhitelist.add(whiteList);
        histogramKvWhitelistSet.add(whiteList);
    }

    /**
     * Adds a key to the blacklist for histogram creation.
     * Keys in the blacklist will be excluded from histogram creation.
     * This method is called by Logback when parsing XML configuration.
     *
     * @param blackList the key to add to the histogram blacklist
     */
    public void addHistogramKvBlacklist(String blackList) {
        this.histogramKvBlacklist.add(blackList);
        histogramKvBlacklistSet.add(blackList);
    }

    /**
     * Cache key for zero-allocation lookups on the hot path.
     * Uses the raw message template (not formatted) and a hash of all tag key-value pairs.
     */
    record CacheKey(String message, int tagHash) {}

    @Override
    public void start() {
        super.start();

        if (!enableSelfObservability) return;

        // Register self-observability metrics
        String prefix = "logback.to.metrics.appender";
        appendTimer = Metrics.timer(prefix + ".append.duration");
        countersCreatedCounter = Metrics.counter(prefix + ".counters.created");
        histogramsCreatedCounter = Metrics.counter(prefix + ".histograms.created");
        Metrics.gauge(prefix + ".counters.active", counters, ConcurrentHashMap::size);
        Metrics.gauge(prefix + ".histograms.active", histograms, ConcurrentHashMap::size);
        cardinalityBlacklistedCounter = Metrics.counter(prefix + ".cardinality.blacklisted");
        reregisterTimer = Metrics.timer(prefix + ".cardinality.reregister.duration");
        Metrics.gauge(prefix + ".counters.saturated", this, a -> a.isCountersSaturated() ? 1 : 0);
        eventsDroppedCounter = Metrics.counter(prefix + ".events.dropped");
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        Timer.Sample sample = (appendTimer != null) ? Timer.start() : null;
        try {
            appendInternal(eventObject);
        } finally {
            if (sample != null && appendTimer != null) {
                sample.stop(appendTimer);
            }
        }
    }

    private void appendInternal(ILoggingEvent eventObject) {
        // Circuit breaker: skip everything if both counters and histograms are saturated
        if (countersSaturated && (!enableAutoHistograms || histogramsSaturated)) {
            if (eventsDroppedCounter != null) eventsDroppedCounter.increment();
            return;
        }

        // Compute cache key with minimal allocation (hash only, no string concat)
        String message = eventObject.getMessage();
        int tagHash = computeTagHash(eventObject);
        var key = new CacheKey(message, tagHash);

        // Hot path: counter already exists — just increment, no tag materialization
        Counter counter = counters.get(key);
        if (counter != null) {
            counter.increment();
        } else if (!countersSaturated) {
            // Cold path: materialize tags and register counter
            registerCounter(eventObject, message, key);
        } else {
            if (eventsDroppedCounter != null) eventsDroppedCounter.increment();
        }

        // Histogram processing
        if (enableAutoHistograms && !histogramsSaturated) {
            processHistograms(eventObject, key);
        }
    }

    /**
     * Computes a hash of all tag-contributing data without allocating any objects.
     * Combines: MDC properties, structured arguments, logstash markers, level, logger, thread.
     */
    private int computeTagHash(ILoggingEvent eventObject) {
        int hash = 17;

        // MDC properties
        var mdcMap = eventObject.getMDCPropertyMap();
        if (mdcMap != null) {
            for (var entry : mdcMap.entrySet()) {
                if (isTagKey(entry.getKey())) {
                    hash = 31 * hash + entry.getKey().hashCode();
                    hash = 31 * hash + entry.getValue().hashCode();
                }
            }
        }

        // Structured arguments
        Object[] args = eventObject.getArgumentArray();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof SingleFieldAppendingMarker sfm) {
                    String fieldName = sfm.getFieldName();
                    if (isTagKey(fieldName)) {
                        hash = 31 * hash + fieldName.hashCode();
                        hash = 31 * hash + sfm.toString().hashCode();
                    }
                }
            }
        }

        // LogstashMarkers — use extractFieldValue to get only each marker's own value
        Marker marker = eventObject.getMarker();
        if (marker instanceof SingleFieldAppendingMarker sfm) {
            if (isTagKey(sfm.getFieldName())) {
                hash = 31 * hash + sfm.getFieldName().hashCode();
                hash = 31 * hash + extractFieldValue(sfm).hashCode();
            }
        }
        if (marker != null && marker.hasReferences()) {
            Iterator<Marker> iter = marker.iterator();
            while (iter.hasNext()) {
                Marker child = iter.next();
                if (child instanceof SingleFieldAppendingMarker sfm) {
                    if (isTagKey(sfm.getFieldName())) {
                        hash = 31 * hash + sfm.getFieldName().hashCode();
                        hash = 31 * hash + extractFieldValue(sfm).hashCode();
                    }
                }
            }
        }

        // Fixed tags
        hash = 31 * hash + eventObject.getLevel().hashCode();
        hash = 31 * hash + eventObject.getLoggerName().hashCode();
        hash = 31 * hash + eventObject.getThreadName().hashCode();

        return hash;
    }

    /**
     * Cold path: materializes tags and registers a new counter.
     */
    private void registerCounter(ILoggingEvent eventObject, String message, CacheKey key) {
        if (counters.size() >= maxCounters) {
            countersSaturated = true;
            if (eventsDroppedCounter != null) eventsDroppedCounter.increment();
            return;
        }

        List<Tag> tags = buildWhitelistedTags(eventObject);
        String counterName = buildMetricName(message, counterNameSubfix);

        Counter counter = counters.computeIfAbsent(key,
                k -> {
                    if (countersCreatedCounter != null) countersCreatedCounter.increment();
                    return Metrics.counter(counterName, tags);
                });
        counter.increment();

        // Cardinality tracking on cold path only
        if (enableCardinalityProtection) {
            trackCardinality(tags);
        }
    }

    /**
     * Builds the tag list by extracting from MDC, StructuredArguments, and LogstashMarkers.
     * No JSON encoding/parsing involved.
     */
    private List<Tag> buildWhitelistedTags(ILoggingEvent eventObject) {
        var mdcMap = eventObject.getMDCPropertyMap();
        Object[] args = eventObject.getArgumentArray();
        int estimatedSize = (mdcMap != null ? mdcMap.size() : 0) + (args != null ? args.length : 0) + 3;
        var tags = new ArrayList<Tag>(estimatedSize);

        // 1. MDC properties
        if (mdcMap != null) {
            for (var entry : mdcMap.entrySet()) {
                if (isTagKey(entry.getKey())) {
                    tags.add(Tag.of(entry.getKey(), entry.getValue()));
                }
            }
        }

        // 2. Structured arguments (from StructuredArguments.kv(), etc.)
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof SingleFieldAppendingMarker sfm) {
                    String fieldName = sfm.getFieldName();
                    if (isTagKey(fieldName)) {
                        tags.add(Tag.of(fieldName, extractFieldValue(sfm)));
                    }
                }
            }
        }

        // 3. LogstashMarkers (from Markers.append(), etc.)
        Marker marker = eventObject.getMarker();
        extractMarkerTags(marker, tags);

        // 4. Fixed tags
        tags.add(Tag.of("level", eventObject.getLevel().toString()));
        tags.add(Tag.of("logger_name", eventObject.getLoggerName()));
        tags.add(Tag.of("thread_name", eventObject.getThreadName()));
        return tags;
    }

    /**
     * Extracts tag key-value pairs from a marker and its children.
     */
    private void extractMarkerTags(Marker marker, List<Tag> tags) {
        if (marker == null) return;

        if (marker instanceof SingleFieldAppendingMarker sfm) {
            if (isTagKey(sfm.getFieldName())) {
                tags.add(Tag.of(sfm.getFieldName(), extractFieldValue(sfm)));
            }
        }

        if (marker.hasReferences()) {
            Iterator<Marker> iter = marker.iterator();
            while (iter.hasNext()) {
                Marker child = iter.next();
                if (child instanceof SingleFieldAppendingMarker sfm) {
                    if (isTagKey(sfm.getFieldName())) {
                        tags.add(Tag.of(sfm.getFieldName(), extractFieldValue(sfm)));
                    }
                }
            }
        }
    }

    /**
     * Extracts the field value from a SingleFieldAppendingMarker.
     * Since getFieldValue() is protected, we use toString() which returns "fieldName=fieldValue".
     * When the marker has chained references, toString() appends them (e.g. "region=us-east-1, env=prod"),
     * so we truncate at the first ", " after the "=" to get only this marker's own value.
     */
    private String extractFieldValue(SingleFieldAppendingMarker marker) {
        String fieldName = marker.getFieldName();
        String full = marker.toString();
        String prefix = fieldName + "=";
        if (full.startsWith(prefix)) {
            String valueAndRest = full.substring(prefix.length());
            // If marker has references, toString() appends them after ", "
            if (marker.hasReferences()) {
                int commaIdx = valueAndRest.indexOf(", ");
                if (commaIdx >= 0) {
                    return valueAndRest.substring(0, commaIdx);
                }
            }
            return valueAndRest;
        }
        return full;
    }

    /**
     * Builds a metric name from the raw message template.
     * Single-pass character loop: removes dots, replaces spaces with counterJoinString.
     */
    private String buildMetricName(String message, String suffix) {
        var sb = new StringBuilder(counterNamePrefix.length() + message.length() + suffix.length() + 2);
        sb.append(counterNamePrefix).append('.');
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '.') continue;
            if (c == ' ') sb.append(counterJoinString);
            else sb.append(c);
        }
        sb.append('.').append(suffix);
        return sb.toString();
    }

    private boolean isTagKey(String key) {
        // Cardinality protection gate-check (NOT applied to histograms)
        if (enableCardinalityProtection && autoBlacklistedKeys.contains(key)) {
            return false;
        }

        if (!kvWhitelistSet.isEmpty()) {
            return kvWhitelistSet.contains(key) && !kvBlacklistSet.contains(key);
        }
        return !kvBlacklistSet.contains(key);
    }

    /**
     * Tracks cardinality of tag keys. Called on cold path only (counter registration).
     * When a key exceeds maxTagValueCardinality, it is auto-blacklisted and existing
     * counters containing that tag are re-registered without it.
     */
    private void trackCardinality(List<Tag> tags) {
        for (Tag tag : tags) {
            String key = tag.getKey();

            // Skip fixed tags — they have bounded cardinality
            if (FIXED_TAG_KEYS.contains(key)) continue;

            // Skip already-blacklisted keys
            if (autoBlacklistedKeys.contains(key)) continue;

            Set<String> values = cardinalityTracker.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            values.add(tag.getValue());

            if (values.size() > maxTagValueCardinality) {
                // Blacklist the key FIRST — all concurrent append() calls will immediately
                // see the key as blacklisted via isTagKey()
                autoBlacklistedKeys.add(key);

                // Free tracking memory for this key
                cardinalityTracker.remove(key);

                addWarn("Auto-blacklisted high-cardinality tag key '" + key
                        + "' (" + values.size() + " distinct values, limit=" + maxTagValueCardinality + ")");

                if (cardinalityBlacklistedCounter != null) cardinalityBlacklistedCounter.increment();

                // Re-register existing counters without the blacklisted tag
                reRegisterCountersWithoutKey(key);
            }
        }
    }

    /**
     * Removes existing counters that contain the blacklisted tag key from the registry
     * and re-registers them without that tag. Counter counts are transferred.
     * This is a cold-path operation — happens once per blacklisted key.
     * Histograms are NOT affected.
     */
    private void reRegisterCountersWithoutKey(String blacklistedKey) {
        if (!reRegisterLock.tryLock()) {
            // Another re-registration is in progress — skip, the key is already
            // in autoBlacklistedKeys so isTagKey() will return false
            return;
        }
        try {
            Timer.Sample sample = (reregisterTimer != null) ? Timer.start() : null;
            try {
                var counterEntries = new ArrayList<>(counters.entrySet());
                for (var entry : counterEntries) {
                    Counter oldCounter = entry.getValue();
                    List<Tag> oldTags = oldCounter.getId().getTags();

                    if (oldTags.stream().noneMatch(t -> t.getKey().equals(blacklistedKey))) continue;

                    // Snapshot count BEFORE removing
                    double count = oldCounter.count();

                    // Remove from our map FIRST
                    counters.remove(entry.getKey());

                    // Remove from Micrometer registry
                    Metrics.globalRegistry.remove(oldCounter);

                    // Build new tags without the blacklisted key
                    List<Tag> newTags = new ArrayList<>();
                    for (Tag t : oldTags) {
                        if (!t.getKey().equals(blacklistedKey)) {
                            newTags.add(t);
                        }
                    }

                    String name = oldCounter.getId().getName();

                    // Compute a new CacheKey using the tag hash from the new tags
                    int newTagHash = computeTagHashFromTags(newTags);
                    CacheKey newKey = new CacheKey(name.substring(counterNamePrefix.length() + 1,
                            name.length() - counterNameSubfix.length() - 1), newTagHash);

                    Counter newCounter = counters.computeIfAbsent(newKey,
                            k -> Metrics.counter(name, newTags));
                    // Transfer the snapshotted count
                    if (count > 0) {
                        newCounter.increment(count);
                    }
                }
            } finally {
                if (sample != null && reregisterTimer != null) {
                    sample.stop(reregisterTimer);
                }
            }
        } finally {
            reRegisterLock.unlock();
        }
    }

    /**
     * Computes a tag hash from a materialized list of tags (used by re-registration).
     */
    private int computeTagHashFromTags(List<Tag> tags) {
        int hash = 17;
        for (Tag tag : tags) {
            hash = 31 * hash + tag.getKey().hashCode();
            hash = 31 * hash + tag.getValue().hashCode();
        }
        return hash;
    }

    /**
     * Processes histograms for numeric values from MDC, structured arguments, and markers.
     */
    private void processHistograms(ILoggingEvent eventObject, CacheKey counterKey) {
        String message = eventObject.getMessage();

        // MDC properties
        var mdcMap = eventObject.getMDCPropertyMap();
        if (mdcMap != null) {
            for (var entry : mdcMap.entrySet()) {
                if (isHistogramKey(entry.getKey())) {
                    recordHistogramFromString(entry.getKey(), entry.getValue(), message, counterKey, eventObject);
                }
            }
        }

        // Structured arguments — can extract native Number values
        Object[] args = eventObject.getArgumentArray();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof SingleFieldAppendingMarker sfm) {
                    String fieldName = sfm.getFieldName();
                    if (isHistogramKey(fieldName)) {
                        recordHistogramFromMarker(sfm, message, counterKey, eventObject);
                    }
                }
            }
        }

        // LogstashMarkers
        Marker marker = eventObject.getMarker();
        if (marker != null) {
            extractHistogramFromMarker(marker, message, counterKey, eventObject);
        }
    }

    private void extractHistogramFromMarker(Marker marker, String message, CacheKey counterKey, ILoggingEvent eventObject) {
        if (marker instanceof SingleFieldAppendingMarker sfm) {
            if (isHistogramKey(sfm.getFieldName())) {
                recordHistogramFromMarker(sfm, message, counterKey, eventObject);
            }
        }
        if (marker.hasReferences()) {
            Iterator<Marker> iter = marker.iterator();
            while (iter.hasNext()) {
                Marker child = iter.next();
                if (child instanceof SingleFieldAppendingMarker sfm) {
                    if (isHistogramKey(sfm.getFieldName())) {
                        recordHistogramFromMarker(sfm, message, counterKey, eventObject);
                    }
                }
            }
        }
    }

    private void recordHistogramFromMarker(SingleFieldAppendingMarker marker, String message, CacheKey counterKey, ILoggingEvent eventObject) {
        String value = extractFieldValue(marker);
        recordHistogramFromString(marker.getFieldName(), value, message, counterKey, eventObject);
    }

    /**
     * Records a histogram value from a string, with fast numeric pre-check.
     * Tags are only materialized on the cold path (first encounter of this histogram key).
     */
    private void recordHistogramFromString(String key, String value, String message, CacheKey counterKey, ILoggingEvent eventObject) {
        Double numericValue = parseNumericValue(value);
        if (numericValue == null) return;

        String histogramName = buildMetricName(message, key + "." + histogramNameSubfix);
        var histKey = new CacheKey(histogramName, counterKey.tagHash());

        DistributionSummary histogram = histograms.get(histKey);
        if (histogram == null) {
            if (histograms.size() >= maxHistograms) {
                histogramsSaturated = true;
                return;
            }
            // Cold path: materialize tags for histogram registration
            List<Tag> tags = buildWhitelistedTags(eventObject);
            histogram = histograms.computeIfAbsent(histKey,
                    k -> {
                        if (histogramsCreatedCounter != null) histogramsCreatedCounter.increment();
                        return DistributionSummary.builder(histogramName)
                                .tags(tags)
                                .register(Metrics.globalRegistry);
                    });
        }
        histogram.record(numericValue);
    }

    private boolean isHistogramKey(String key) {
        if (!histogramKvWhitelistSet.isEmpty()) {
            return histogramKvWhitelistSet.contains(key) && !histogramKvBlacklistSet.contains(key);
        }
        return !histogramKvBlacklistSet.contains(key);
    }

    /**
     * Fast numeric pre-check + parse. Avoids NumberFormatException stack traces
     * for obviously non-numeric strings.
     */
    private static Double parseNumericValue(String value) {
        if (value == null || value.isEmpty()) return null;

        // Fast pre-check: first char must be digit, minus, or dot
        char first = value.charAt(0);
        if (first != '-' && first != '.' && (first < '0' || first > '9')) return null;

        // Also reject whitespace-only (trim only if it passed the first-char check)
        if (value.isBlank()) return null;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
