package io.github.dordor12;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
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
 */
@Getter
@Setter
public class LogbackToMetricsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

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

    // Circuit breaker flags — once saturated, skip all computation
    private volatile boolean countersSaturated = false;
    private volatile boolean histogramsSaturated = false;

    /**
     * Adds a key to the whitelist for metric tag extraction.
     * This method is called by Logback when parsing XML configuration.
     */
    public void addKvWhitelist(String whiteList) {
        this.kvWhitelist.add(whiteList);
        kvWhitelistSet.add(whiteList);
    }

    /**
     * Adds a key to the blacklist for metric tag extraction.
     * This method is called by Logback when parsing XML configuration.
     */
    public void addKvBlacklist(String blackList) {
        this.kvBlacklist.add(blackList);
        kvBlacklistSet.add(blackList);
    }

    /**
     * Adds a key to the whitelist for histogram creation.
     * This method is called by Logback when parsing XML configuration.
     */
    public void addHistogramKvWhitelist(String whiteList) {
        this.histogramKvWhitelist.add(whiteList);
        histogramKvWhitelistSet.add(whiteList);
    }

    /**
     * Adds a key to the blacklist for histogram creation.
     * This method is called by Logback when parsing XML configuration.
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
    protected void append(ILoggingEvent eventObject) {
        // Circuit breaker: skip everything if both counters and histograms are saturated
        if (countersSaturated && (!enableAutoHistograms || histogramsSaturated)) {
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
            return;
        }

        List<Tag> tags = buildWhitelistedTags(eventObject);
        String counterName = buildMetricName(message, counterNameSubfix);

        Counter counter = counters.computeIfAbsent(key,
                k -> Metrics.counter(counterName, tags));
        counter.increment();
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
        if (!kvWhitelistSet.isEmpty()) {
            return kvWhitelistSet.contains(key) && !kvBlacklistSet.contains(key);
        }
        return !kvBlacklistSet.contains(key);
    }

    /**
     * Processes histograms for numeric values from MDC and structured arguments.
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
                    k -> DistributionSummary.builder(histogramName)
                            .tags(tags)
                            .register(Metrics.globalRegistry));
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
