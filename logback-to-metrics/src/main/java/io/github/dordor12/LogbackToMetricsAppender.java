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
}
