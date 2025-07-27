package io.github.dordor12;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import net.logstash.logback.encoder.LogstashEncoder;
import org.apache.commons.codec.digest.DigestUtils;
import lombok.Getter;
import lombok.Setter;


/**
 * A Logback appender that converts log events into Micrometer metrics.
 * This appender extracts key-value pairs from log events and creates counters
 * based on configurable whitelist and blacklist filters.
 */
@Getter
@Setter
public class LogbackToMetricsAppender extends OutputStreamAppender<ILoggingEvent> {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private List<String> kvWhitelist = new ArrayList<>();
    private List<String> kvBlacklist = new ArrayList<>();
    private Map<String, Boolean> kvWhitelistMap = new HashMap<>();
    private Map<String, Boolean> kvBlacklistMap = new HashMap<>();
    private ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private Long maxCounters = 10000L;
    private String counterJoinString = ".";
    private String counterNamePrefix = "logback.to.metrics";
    private String counterNameSubfix = "counter";
    
    // Histogram configuration
    private boolean enableAutoHistograms = false;
    private List<String> histogramKvWhitelist = new ArrayList<>();
    private List<String> histogramKvBlacklist = new ArrayList<>();
    private Map<String, Boolean> histogramKvWhitelistMap = new HashMap<>();
    private Map<String, Boolean> histogramKvBlacklistMap = new HashMap<>();
    private ConcurrentHashMap<String, DistributionSummary> histograms = new ConcurrentHashMap<>();
    private Long maxHistograms = 10000L;
    private String histogramNameSubfix = "histogram";

    /**
     * Default constructor that initializes the appender with an empty output stream.
     * The actual output is handled by creating metrics instead of writing to a stream.
     */
    public LogbackToMetricsAppender() {
        this.setOutputStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // empty output stream
            }
        });
    }

    /**
     * Adds a key to the whitelist for metric tag extraction.
     * Only keys in the whitelist will be included as metric tags.
     * This method is called by Logback when parsing XML configuration.
     *
     * @param whiteList the key to add to the whitelist
     */
    public void addKvWhitelist(String whiteList) {
        this.kvWhitelist.add(whiteList);
        kvWhitelistMap.put(whiteList, true);
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
        kvBlacklistMap.put(blackList, true);
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
        histogramKvWhitelistMap.put(whiteList, true);
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
        histogramKvBlacklistMap.put(blackList, true);
    }


    @Override
    public void append(ILoggingEvent eventObject) {
        var counterName = getCounterName(eventObject);
        var tags = getWhitelistedTags(eventObject);
        var counterId = getCounterId(counterName, tags);
        if (!counters.containsKey(counterId)) {
            if (counters.size() >= maxCounters) {
                return;
            }
            counters.put(counterId, Metrics.counter(getCounterName(eventObject), getWhitelistedTags(eventObject)));
        }
        counters.get(counterId).increment();
        
        // Create histograms for numeric values
        createHistograms(eventObject);
    }

    private String getCounterId(String counterName, List<Tag> tags) {
        var tagsId = tags.stream()
                .map(tag -> String.format("%s=%s", tag.getKey(), tag.getValue()))
                .collect(Collectors.joining(counterJoinString));

        return String.format("%s.%s", counterName, DigestUtils.md5Hex(tagsId));
    }

    private List<Tag> getWhitelistedTags(ILoggingEvent eventObject) {
        var tags = eventObject.getMDCPropertyMap()
                .entrySet()
                .stream()
                .filter(this::isTagKey)
                .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        var encoder = this.getEncoder();
        if (encoder instanceof LogstashEncoder) {
            var encoded = encoder.encode(eventObject);
            try {
                var json = objectMapper.readTree(encoded);
                tags.addAll(
                        json.properties().stream()
                                .filter(entry -> !entry.getKey().equals("@timestamp"))
                                .filter(entry -> !entry.getValue().isObject() && isTagKey(entry))
                                .map(entry -> Tag.of(entry.getKey(), getJsonValue(entry.getValue())))
                                .toList()
                );
            } catch (IOException e) {
                addError("failed to read logstash encoder json", e);
            }
        }
        tags.add(Tag.of("level", eventObject.getLevel().toString()));
        tags.add(Tag.of("logger_name", eventObject.getLoggerName()));
        tags.add(Tag.of("thread_name", eventObject.getThreadName()));
        return tags;
    }

    private String getJsonValue(JsonNode value) {
        if (value.isArray()) {
            Iterator<JsonNode> iterator = ((ArrayNode) value).elements();
            Iterable<JsonNode> iterable = () -> iterator;
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(this::getJsonValue)
                    .collect(Collectors.joining(","));
        }
        return value.asText();
    }

    private String getCounterName(ILoggingEvent eventObject) {
        return String.format("%s.%s.%s", counterNamePrefix, eventObject.getMessage().replaceAll("\\.", "").replaceAll(" ", counterJoinString), counterNameSubfix);
    }

    private boolean isTagKey(Map.Entry<String, ?> entry) {
        if (!kvWhitelistMap.isEmpty())
            return kvWhitelistMap.containsKey(entry.getKey()) && kvWhitelistMap.get(entry.getKey()) && !kvBlacklistMap.containsKey(entry.getKey());
        return !kvBlacklistMap.containsKey(entry.getKey());
    }

    private void createHistograms(ILoggingEvent eventObject) {
        // Only create histograms if the feature is enabled
        if (!enableAutoHistograms) {
            return;
        }
        
        // Process MDC properties
        eventObject.getMDCPropertyMap()
                .entrySet()
                .stream()
                .filter(this::isHistogramKey)
                .forEach(entry -> createHistogramForNumericValue(eventObject, entry.getKey(), entry.getValue()));

        // Process LogstashEncoder JSON properties
        var encoder = this.getEncoder();
        if (encoder instanceof LogstashEncoder) {
            var encoded = encoder.encode(eventObject);
            try {
                var json = objectMapper.readTree(encoded);
                json.properties().stream()
                        .filter(entry -> !entry.getKey().equals("@timestamp"))
                        .filter(entry -> !entry.getValue().isObject())
                        .filter(this::isHistogramKey)
                        .forEach(entry -> createHistogramForNumericValue(eventObject, entry.getKey(), getJsonValue(entry.getValue())));
            } catch (IOException e) {
                addError("failed to read logstash encoder json for histogram creation", e);
            }
        }
    }

    private boolean isHistogramKey(Map.Entry<String, ?> entry) {
        if (!histogramKvWhitelistMap.isEmpty())
            return histogramKvWhitelistMap.containsKey(entry.getKey()) && histogramKvWhitelistMap.get(entry.getKey()) && !histogramKvBlacklistMap.containsKey(entry.getKey());
        return !histogramKvBlacklistMap.containsKey(entry.getKey());
    }

    private void createHistogramForNumericValue(ILoggingEvent eventObject, String key, String value) {
        Double numericValue = parseNumericValue(value);
        if (numericValue != null) {
            var histogramName = getHistogramName(eventObject, key);
            var tags = getWhitelistedTags(eventObject);
            var histogramId = getCounterId(histogramName, tags);
            
            if (!histograms.containsKey(histogramId)) {
                if (histograms.size() >= maxHistograms) {
                    return;
                }
                histograms.put(histogramId, DistributionSummary.builder(histogramName)
                        .tags(tags)
                        .register(Metrics.globalRegistry));
            }
            histograms.get(histogramId).record(numericValue);
        }
    }

    private String getHistogramName(ILoggingEvent eventObject, String key) {
        return String.format("%s.%s.%s.%s", counterNamePrefix, 
                eventObject.getMessage().replaceAll("\\.", "").replaceAll(" ", counterJoinString), 
                key, histogramNameSubfix);
    }

    private Double parseNumericValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as different numeric types
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                // For integer values, convert to double
                return Double.valueOf(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
