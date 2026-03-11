package io.github.dordor12;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import net.logstash.logback.encoder.LogstashEncoder;
import lombok.Getter;
import lombok.Setter;


/**
 * A Logback appender that converts log events into Micrometer metrics.
 * This appender extracts key-value pairs from log events and creates counters
 * based on configurable whitelist and blacklist filters.
 */
@Getter
@Setter
public class LogbackToMetricsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Encoder<ILoggingEvent> encoder;
    private List<String> kvWhitelist = new ArrayList<>();
    private List<String> kvBlacklist = new ArrayList<>();
    private Set<String> kvWhitelistSet = new HashSet<>();
    private Set<String> kvBlacklistSet = new HashSet<>();
    private ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
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
    private ConcurrentHashMap<String, DistributionSummary> histograms = new ConcurrentHashMap<>();
    private Long maxHistograms = 10000L;
    private String histogramNameSubfix = "histogram";

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

    @Override
    public void start() {
        if (encoder != null) {
            encoder.start();
        }
        super.start();
    }

    /**
     * Per-event context that caches all derived data to avoid redundant computation.
     */
    private static class EventContext {
        final String counterName;
        final List<Tag> tags;
        final String counterId;
        final JsonNode logstashJson;

        EventContext(String counterName, List<Tag> tags, String counterId, JsonNode logstashJson) {
            this.counterName = counterName;
            this.tags = tags;
            this.counterId = counterId;
            this.logstashJson = logstashJson;
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        var ctx = buildEventContext(eventObject);

        Counter counter = counters.get(ctx.counterId);
        if (counter == null) {
            if (counters.size() >= maxCounters) {
                return;
            }
            counter = counters.computeIfAbsent(ctx.counterId,
                    k -> Metrics.counter(ctx.counterName, ctx.tags));
        }
        counter.increment();

        // Create histograms for numeric values
        if (enableAutoHistograms) {
            createHistograms(eventObject, ctx);
        }
    }

    private EventContext buildEventContext(ILoggingEvent eventObject) {
        String counterName = getCounterName(eventObject);
        JsonNode logstashJson = null;

        var enc = getEncoder();
        if (enc instanceof LogstashEncoder) {
            byte[] encoded = enc.encode(eventObject);
            try {
                logstashJson = objectMapper.readTree(encoded);
            } catch (IOException e) {
                addError("failed to read logstash encoder json", e);
            }
        }

        List<Tag> tags = buildWhitelistedTags(eventObject, logstashJson);
        String counterId = getCounterId(counterName, tags);

        return new EventContext(counterName, tags, counterId, logstashJson);
    }

    private String getCounterId(String counterName, List<Tag> tags) {
        var sb = new StringBuilder(counterName.length() + tags.size() * 20);
        sb.append(counterName);
        for (Tag tag : tags) {
            sb.append('.').append(tag.getKey()).append('=').append(tag.getValue());
        }
        return sb.toString();
    }

    private List<Tag> buildWhitelistedTags(ILoggingEvent eventObject, JsonNode logstashJson) {
        var mdcMap = eventObject.getMDCPropertyMap();
        var tags = new ArrayList<Tag>(mdcMap.size() + 3);

        for (var entry : mdcMap.entrySet()) {
            if (isTagKey(entry.getKey())) {
                tags.add(Tag.of(entry.getKey(), entry.getValue()));
            }
        }

        if (logstashJson != null) {
            for (var entry : logstashJson.properties()) {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (!"@timestamp".equals(key) && !value.isObject() && isTagKey(key)) {
                    tags.add(Tag.of(key, getJsonValue(value)));
                }
            }
        }

        tags.add(Tag.of("level", eventObject.getLevel().toString()));
        tags.add(Tag.of("logger_name", eventObject.getLoggerName()));
        tags.add(Tag.of("thread_name", eventObject.getThreadName()));
        return tags;
    }

    private String getJsonValue(JsonNode value) {
        if (value.isArray()) {
            var sb = new StringBuilder();
            Iterator<JsonNode> iterator = ((ArrayNode) value).elements();
            boolean first = true;
            while (iterator.hasNext()) {
                if (!first) sb.append(',');
                sb.append(getJsonValue(iterator.next()));
                first = false;
            }
            return sb.toString();
        }
        return value.asText();
    }

    private String getCounterName(ILoggingEvent eventObject) {
        String msg = eventObject.getMessage();
        var sb = new StringBuilder(counterNamePrefix.length() + msg.length() + counterNameSubfix.length() + 2);
        sb.append(counterNamePrefix).append('.');
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '.') continue;
            if (c == ' ') sb.append(counterJoinString);
            else sb.append(c);
        }
        sb.append('.').append(counterNameSubfix);
        return sb.toString();
    }

    private boolean isTagKey(String key) {
        if (!kvWhitelistSet.isEmpty()) {
            return kvWhitelistSet.contains(key) && !kvBlacklistSet.contains(key);
        }
        return !kvBlacklistSet.contains(key);
    }

    private void createHistograms(ILoggingEvent eventObject, EventContext ctx) {
        // Process MDC properties
        for (var entry : eventObject.getMDCPropertyMap().entrySet()) {
            if (isHistogramKey(entry.getKey())) {
                createHistogramForNumericValue(entry.getKey(), entry.getValue(), eventObject, ctx);
            }
        }

        // Process LogstashEncoder JSON properties
        if (ctx.logstashJson != null) {
            for (var entry : ctx.logstashJson.properties()) {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (!"@timestamp".equals(key) && !value.isObject() && isHistogramKey(key)) {
                    createHistogramForNumericValue(key, getJsonValue(value), eventObject, ctx);
                }
            }
        }
    }

    private boolean isHistogramKey(String key) {
        if (!histogramKvWhitelistSet.isEmpty()) {
            return histogramKvWhitelistSet.contains(key) && !histogramKvBlacklistSet.contains(key);
        }
        return !histogramKvBlacklistSet.contains(key);
    }

    private void createHistogramForNumericValue(String key, String value, ILoggingEvent eventObject, EventContext ctx) {
        Double numericValue = parseNumericValue(value);
        if (numericValue != null) {
            var histogramName = getHistogramName(eventObject, key);
            var histogramId = getCounterId(histogramName, ctx.tags);

            DistributionSummary histogram = histograms.get(histogramId);
            if (histogram == null) {
                if (histograms.size() >= maxHistograms) {
                    return;
                }
                histogram = histograms.computeIfAbsent(histogramId,
                        k -> DistributionSummary.builder(histogramName)
                                .tags(ctx.tags)
                                .register(Metrics.globalRegistry));
            }
            histogram.record(numericValue);
        }
    }

    private String getHistogramName(ILoggingEvent eventObject, String key) {
        String msg = eventObject.getMessage();
        var sb = new StringBuilder(counterNamePrefix.length() + msg.length() + key.length() + histogramNameSubfix.length() + 3);
        sb.append(counterNamePrefix).append('.');
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '.') continue;
            if (c == ' ') sb.append(counterJoinString);
            else sb.append(c);
        }
        sb.append('.').append(key).append('.').append(histogramNameSubfix);
        return sb.toString();
    }

    private Double parseNumericValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Double.valueOf(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
