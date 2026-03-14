package io.github.dordor12;

import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.marker.Markers.append;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogbackToMetricsAppenderTest {

    private LogbackToMetricsAppender appender;
    private SimpleMeterRegistry registry;

    @BeforeEach
    public void setUp() {
        appender = new LogbackToMetricsAppender();
        appender.setEnableAutoHistograms(true);

        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterEach
    public void afterEach() {
        registry.clear();
        Metrics.removeRegistry(registry);
    }

    private ILoggingEvent mockBasicEvent(String msg) {
        var event = mock(ILoggingEvent.class);
        when(event.getMessage()).thenReturn(msg);
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("IncrementLogger");
        when(event.getThreadName()).thenReturn("IncrementThread");
        when(event.getMDCPropertyMap()).thenReturn(Map.of());
        return event;
    }

    // === Counter tests ===

    @Test
    public void testCounterIncrement() {
        var event = mockBasicEvent("basic counter event");

        appender.append(event);
        appender.append(event);

        assertEquals(2.0, registry.get("logback.to.metrics.basic.counter.event.counter").counter().count());
    }

    @Test
    public void testCounterIncrementCustomName() {
        var event = mockBasicEvent("basic counter event rename");
        appender.setCounterNamePrefix("custom.name");

        appender.append(event);
        appender.append(event);

        assertEquals(2.0, registry.get("custom.name.basic.counter.event.rename.counter").counter().count());
    }

    @Test
    public void testMaxCounters() {
        var event = mockBasicEvent("my event");
        var event1 = mockBasicEvent("Increment Test1");
        var event2 = mockBasicEvent("Increment Test2");
        appender.setMaxCounters(2L);

        appender.append(event);
        appender.append(event1);
        appender.append(event2);
        appender.append(event); // Increment existing counter

        assertEquals(2.0, registry.get("logback.to.metrics.my.event.counter").counter().count());
        assertEquals(1.0, registry.get("logback.to.metrics.Increment.Test1.counter").counter().count());
        assertNull(registry.find("logback.to.metrics.Increment.Test2.counter").counter());
    }

    @Test
    public void testMaxCountersWhenTagsChange() {
        var event = mock(ILoggingEvent.class);
        when(event.getMessage()).thenReturn("basic event");
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("IncrementLogger");
        when(event.getThreadName()).thenReturn("IncrementThread");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"))
                .thenReturn(Map.of("key1", "value2"));

        var event1 = mockBasicEvent("Increment Test1");
        appender.setMaxCounters(2L);

        appender.append(event);
        appender.append(event1);
        appender.append(event); // Different tags = different counter

        assertEquals(1.0, registry.get("logback.to.metrics.basic.event.counter").counter().count());
        assertEquals(1.0, registry.get("logback.to.metrics.Increment.Test1.counter").counter().count());
    }

    // === MDC Tag tests ===

    @Test
    public void testAddTags() {
        var event = mockBasicEvent("basic event tags");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));

        appender.append(event);
        appender.append(event);

        assertEquals(2.0, registry.get("logback.to.metrics.basic.event.tags.counter").counter().count());
        List<Tag> tags = registry.get("logback.to.metrics.basic.event.tags.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenNotInWhiteListShouldNotAdded() {
        var event = mockBasicEvent("white and black list1");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));
        appender.addKvWhitelist("bla");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list1.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1")));
    }

    @Test
    public void testAddTagsWhenInWhiteListShouldAdded() {
        var event = mockBasicEvent("white and black list2");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));
        appender.addKvWhitelist("key1");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list2.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenNotInBlackListShouldAdded() {
        var event = mockBasicEvent("white and black list3");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("bla");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list3.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenInBlackListShouldNotAdded() {
        var event = mockBasicEvent("white and black list4");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("key1");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list4.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1")));
    }

    @Test
    public void testAddTagsWhenInBlackListAndInWhitelistShouldNotAdded() {
        var event = mockBasicEvent("white and black list5");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("key1");
        appender.addKvWhitelist("key1");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list5.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1")));
    }

    // === Structured Argument tests ===

    @Test
    public void testStructuredArgumentExtraction() {
        var event = mockBasicEvent("structured arg test");
        var kvArg = kv("endpoint", "/api/v1");
        when(event.getArgumentArray()).thenReturn(new Object[]{kvArg});

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.structured.arg.test.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("endpoint") && a.getValue().equals("/api/v1")));
    }

    @Test
    public void testStructuredArgumentWhitelistFiltering() {
        var event = mockBasicEvent("sa filter test");
        when(event.getArgumentArray()).thenReturn(new Object[]{kv("allowed", "yes"), kv("blocked", "no")});
        appender.addKvWhitelist("allowed");

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.sa.filter.test.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("allowed")));
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("blocked")));
    }

    // === LogstashMarker tests ===

    @Test
    public void testLogstashMarkerExtraction() {
        var event = mockBasicEvent("marker test");
        var marker = append("region", "us-east-1");
        when(event.getMarker()).thenReturn(marker);

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.marker.test.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("region") && a.getValue().equals("us-east-1")));
    }

    @Test
    public void testLogstashMarkerChainExtraction() {
        var event = mockBasicEvent("marker chain test");
        var marker = append("region", "us-east-1").and(append("env", "prod"));
        when(event.getMarker()).thenReturn(marker);

        appender.append(event);

        List<Tag> tags = registry.get("logback.to.metrics.marker.chain.test.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("region") && a.getValue().equals("us-east-1")));
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("env") && a.getValue().equals("prod")));
    }

    // === Histogram tests ===

    @Test
    public void testHistogramCreatedForNumericValueInMDC() {
        var event = mockBasicEvent("numeric test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("response_time", "123"));

        appender.append(event);
        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.numeric.test.response_time.histogram").summary();
        assertEquals(2, histogram.count());
        assertEquals(246.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramCreatedForDoubleValue() {
        var event = mockBasicEvent("double test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("cpu_usage", "45.67"));

        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.double.test.cpu_usage.histogram").summary();
        assertEquals(1, histogram.count());
        assertEquals(45.67, histogram.totalAmount(), 0.01);
    }

    @Test
    public void testHistogramNotCreatedForNonNumericValue() {
        var event = mockBasicEvent("non numeric test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("user_name", "john_doe"));

        appender.append(event);

        assertNull(registry.find("logback.to.metrics.non.numeric.test.user_name.histogram").summary());
    }

    @Test
    public void testHistogramFromStructuredArgument() {
        var event = mockBasicEvent("sa histogram test");
        when(event.getArgumentArray()).thenReturn(new Object[]{kv("duration", 500)});
        appender.addHistogramKvWhitelist("duration");

        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.sa.histogram.test.duration.histogram").summary();
        assertEquals(1, histogram.count());
        assertEquals(500.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramWhitelistFiltering() {
        var event = mockBasicEvent("whitelist test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("allowed_metric", "100", "blocked_metric", "200"));
        appender.addHistogramKvWhitelist("allowed_metric");

        appender.append(event);

        assertNotNull(registry.find("logback.to.metrics.whitelist.test.allowed_metric.histogram").summary());
        assertNull(registry.find("logback.to.metrics.whitelist.test.blocked_metric.histogram").summary());
    }

    @Test
    public void testHistogramBlacklistFiltering() {
        var event = mockBasicEvent("blacklist test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("allowed_metric", "100", "blocked_metric", "200"));
        appender.addHistogramKvBlacklist("blocked_metric");

        appender.append(event);

        assertNotNull(registry.find("logback.to.metrics.blacklist.test.allowed_metric.histogram").summary());
        assertNull(registry.find("logback.to.metrics.blacklist.test.blocked_metric.histogram").summary());
    }

    @Test
    public void testHistogramBlacklistOverridesWhitelist() {
        var event = mockBasicEvent("override test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("conflicted_metric", "100"));
        appender.addHistogramKvWhitelist("conflicted_metric");
        appender.addHistogramKvBlacklist("conflicted_metric");

        appender.append(event);

        assertNull(registry.find("logback.to.metrics.override.test.conflicted_metric.histogram").summary());
    }

    @Test
    public void testMaxHistogramsLimit() {
        appender.setMaxHistograms(2L);

        var event1 = mockBasicEvent("limit test 1");
        when(event1.getMDCPropertyMap()).thenReturn(Map.of("metric1", "100"));

        var event2 = mockBasicEvent("limit test 2");
        when(event2.getMDCPropertyMap()).thenReturn(Map.of("metric2", "200"));

        var event3 = mockBasicEvent("limit test 3");
        when(event3.getMDCPropertyMap()).thenReturn(Map.of("metric3", "300"));

        appender.append(event1);
        appender.append(event2);
        appender.append(event3); // Should be ignored due to limit

        assertNotNull(registry.find("logback.to.metrics.limit.test.1.metric1.histogram").summary());
        assertNotNull(registry.find("logback.to.metrics.limit.test.2.metric2.histogram").summary());
        assertNull(registry.find("logback.to.metrics.limit.test.3.metric3.histogram").summary());
    }

    @Test
    public void testMaxHistogramsLimitPreventOOM() {
        registry.clear();
        appender.setEnableAutoHistograms(true);
        appender.setMaxHistograms(3L);
        appender.setMaxCounters(0L); // Disable counters to isolate histogram count

        for (int i = 0; i < 5; i++) {
            var event = mockBasicEvent("limit test " + i);
            when(event.getMDCPropertyMap()).thenReturn(Map.of("metric" + i, String.valueOf(i * 10)));
            appender.append(event);
        }

        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();

        assertTrue(histogramCount <= 3L, "Should have at most 3 histograms, but got " + histogramCount);
    }

    @Test
    public void testHistogramWithDifferentNumericTypes() {
        testNumericType("int_val", "42", 42.0);
        testNumericType("long_val", "9876543210", 9876543210.0);
        testNumericType("double_val", "3.14159", 3.14159);
        testNegativeNumericType("negative_int", "-100");
        testNegativeNumericType("negative_double", "-2.5");
    }

    private void testNumericType(String key, String value, double expectedValue) {
        var event = mockBasicEvent("types test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(key, value));

        appender.append(event);

        DistributionSummary hist = registry.get("logback.to.metrics.types.test." + key + ".histogram").summary();
        assertEquals(1, hist.count(), "Count for " + key);
        assertEquals(expectedValue, hist.totalAmount(), 0.00001, "Total amount for " + key);

        registry.clear();
    }

    private void testNegativeNumericType(String key, String value) {
        var event = mockBasicEvent("types test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(key, value));

        appender.append(event);

        DistributionSummary hist = registry.get("logback.to.metrics.types.test." + key + ".histogram").summary();
        assertEquals(0, hist.count(), "Count for negative " + key + " should be 0");
        assertEquals(0.0, hist.totalAmount(), 0.00001, "Total amount for negative " + key + " should be 0");

        registry.clear();
    }

    @Test
    public void testHistogramIgnoresEmptyAndNullValues() {
        var event = mockBasicEvent("empty test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                "empty_string", "",
                "space_string", "   ",
                "valid_number", "123"
        ));

        appender.append(event);

        assertNull(registry.find("logback.to.metrics.empty.test.empty_string.histogram").summary());
        assertNull(registry.find("logback.to.metrics.empty.test.space_string.histogram").summary());
        assertNotNull(registry.find("logback.to.metrics.empty.test.valid_number.histogram").summary());
    }

    @Test
    public void testSimpleHistogramCreation() {
        var event = mockBasicEvent("simple test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        DistributionSummary histogram = registry.find("logback.to.metrics.simple.test.test_metric.histogram").summary();
        assertNotNull(histogram);
        assertEquals(1, histogram.count());
        assertEquals(42.0, histogram.totalAmount());
    }

    @Test
    public void testNegativeHistogramCreation() {
        var event = mockBasicEvent("negative test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("negative_metric", "-100"));

        appender.append(event);

        DistributionSummary histogram = registry.find("logback.to.metrics.negative.test.negative_metric.histogram").summary();
        assertNotNull(histogram);
        // DistributionSummary ignores negative values, so count should be 0
        assertEquals(0, histogram.count());
        assertEquals(0.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramToggleDisabled() {
        registry.clear();
        appender.setEnableAutoHistograms(false);

        var event = mockBasicEvent("toggle test disabled");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();

        assertEquals(0L, histogramCount);
    }

    @Test
    public void testHistogramToggleEnabled() {
        registry.clear();
        appender.setEnableAutoHistograms(true);

        var event = mockBasicEvent("toggle test enabled");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();

        assertEquals(1L, histogramCount);
    }

    // === Circuit breaker tests ===

    @Test
    public void testCircuitBreakerStopsProcessingWhenCountersSaturated() {
        appender.setMaxCounters(1L);
        appender.setEnableAutoHistograms(false);

        var event1 = mockBasicEvent("first event");
        var event2 = mockBasicEvent("second event");
        var event3 = mockBasicEvent("third event");

        appender.append(event1); // Creates counter
        appender.append(event2); // Saturates, sets flag
        appender.append(event3); // Should be skipped entirely by circuit breaker

        assertEquals(1.0, registry.get("logback.to.metrics.first.event.counter").counter().count());
        // After saturation, no more counters created
        assertNull(registry.find("logback.to.metrics.third.event.counter").counter());
    }

    // === Fast numeric pre-check tests ===

    @Test
    public void testLooksNumericSkipsNonNumericStrings() {
        var event = mockBasicEvent("numeric check test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                "user_name", "john_doe",
                "status", "active",
                "valid_num", "42"
        ));

        appender.append(event);

        // Non-numeric strings should NOT create histograms
        assertNull(registry.find("logback.to.metrics.numeric.check.test.user_name.histogram").summary());
        assertNull(registry.find("logback.to.metrics.numeric.check.test.status.histogram").summary());
        // Numeric string should create histogram
        assertNotNull(registry.find("logback.to.metrics.numeric.check.test.valid_num.histogram").summary());
    }

    // === Concurrency test ===

    @Test
    public void testConcurrentAppendCorrectness() throws InterruptedException {
        int threadCount = 8;
        int eventsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        var event = mockBasicEvent("concurrent event");
                        appender.append(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        Counter counter = registry.get("logback.to.metrics.concurrent.event.counter").counter();
        assertEquals(threadCount * eventsPerThread, (int) counter.count());
    }

    // === Cardinality Protection tests ===

    @Test
    public void testCardinalityProtectionDisabledByDefault() {
        assertFalse(appender.isEnableCardinalityProtection());

        for (int i = 0; i < 200; i++) {
            var event = mockBasicEvent("cardinality disabled test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("userId", "user_" + i));
            appender.append(event);
        }

        assertTrue(appender.getAutoBlacklistedKeys().isEmpty());
    }

    @Test
    public void testCardinalityProtectionAutoBlacklistsKey() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(5);

        for (int i = 0; i < 10; i++) {
            var event = mockBasicEvent("cardinality test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("requestId", "req_" + i));
            appender.append(event);
        }

        assertTrue(appender.getAutoBlacklistedKeys().contains("requestId"));
    }

    @Test
    public void testMetricsReRegisteredWithoutBlacklistedTag() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(3);

        // Create counters WITH the tag that will be blacklisted
        for (int i = 0; i < 3; i++) {
            var event = mockBasicEvent("reregister test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("highCard", "val_" + i, "stableTag", "stable"));
            appender.append(event);
        }

        // At this point, counters exist with "highCard" tag
        long countersWithHighCard = registry.getMeters().stream()
                .filter(m -> m instanceof Counter)
                .filter(m -> m.getId().getName().contains("reregister.test"))
                .filter(m -> m.getId().getTags().stream().anyMatch(t -> t.getKey().equals("highCard")))
                .count();
        assertTrue(countersWithHighCard > 0, "Should have counters with highCard tag before blacklisting");

        // Now trigger blacklisting by exceeding the limit
        var event = mockBasicEvent("reregister test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("highCard", "val_overflow", "stableTag", "stable"));
        appender.append(event);

        assertTrue(appender.getAutoBlacklistedKeys().contains("highCard"));

        // After re-registration, counters in our map should NOT have the highCard tag
        for (var entry : appender.getCounters().values()) {
            if (entry.getId().getName().contains("reregister.test")) {
                assertFalse(entry.getId().getTags().stream().anyMatch(t -> t.getKey().equals("highCard")),
                        "Counter should not have highCard tag after re-registration");
                assertTrue(entry.getId().getTags().stream().anyMatch(t -> t.getKey().equals("stableTag")),
                        "Counter should still have stableTag");
            }
        }

        // New events should create counters without the blacklisted tag
        var newEvent = mockBasicEvent("reregister test");
        when(newEvent.getMDCPropertyMap()).thenReturn(Map.of("highCard", "val_new", "stableTag", "stable"));
        appender.append(newEvent);

        // The new counter should NOT have highCard tag
        for (var entry : appender.getCounters().values()) {
            if (entry.getId().getName().contains("reregister.test")) {
                assertFalse(entry.getId().getTags().stream().anyMatch(t -> t.getKey().equals("highCard")));
            }
        }
    }

    @Test
    public void testCardinalityProtectionDoesNotAffectBoundedKeys() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(50);

        for (int i = 0; i < 100; i++) {
            var event = mockBasicEvent("bounded key test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("env", "prod"));
            appender.append(event);
        }

        assertFalse(appender.getAutoBlacklistedKeys().contains("env"));
    }

    @Test
    public void testFixedTagsNotTracked() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(2);

        for (int i = 0; i < 50; i++) {
            var event = mockBasicEvent("fixed tags test " + i);
            appender.append(event);
        }

        assertFalse(appender.getAutoBlacklistedKeys().contains("level"));
        assertFalse(appender.getAutoBlacklistedKeys().contains("logger_name"));
        assertFalse(appender.getAutoBlacklistedKeys().contains("thread_name"));
    }

    @Test
    public void testCardinalityProtectionDoesNotAffectHistograms() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(3);
        appender.setEnableAutoHistograms(true);

        for (int i = 0; i < 5; i++) {
            var event = mockBasicEvent("histogram cardinality test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("highCard", "val_" + i, "metric_val", "100"));
            appender.append(event);
        }

        assertTrue(appender.getAutoBlacklistedKeys().contains("highCard"));

        // Histograms should still have been created (they use isHistogramKey, not isTagKey)
        assertNotNull(registry.find("logback.to.metrics.histogram.cardinality.test.metric_val.histogram").summary());
    }

    @Test
    public void testCardinalityProtectionConcurrentAccess() throws InterruptedException {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(10);

        int threadCount = 8;
        int eventsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        var event = mockBasicEvent("concurrent cardinality");
                        when(event.getMDCPropertyMap()).thenReturn(
                                Map.of("highCard", "t" + threadId + "_v" + i));
                        appender.append(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // highCard should be auto-blacklisted (8 threads * 50 values >> limit of 10)
        assertTrue(appender.getAutoBlacklistedKeys().contains("highCard"));

        // No exception should have occurred — verify state is consistent
        assertNotNull(appender.getCounters());
    }

    @Test
    public void testCardinalityTrackerMemoryReclaimed() {
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(3);

        for (int i = 0; i < 5; i++) {
            var event = mockBasicEvent("memory test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("memKey", "val_" + i));
            appender.append(event);
        }

        assertTrue(appender.getAutoBlacklistedKeys().contains("memKey"));
        // Tracking set should be removed to free memory
        assertNull(appender.getCardinalityTracker().get("memKey"));
    }

    // === Self-Observability Metrics tests ===

    @Test
    public void testSelfObservabilityDisabled() {
        appender.setEnableSelfObservability(false);
        appender.start();

        // Appender's own self-observability fields should remain null
        assertNull(appender.getAppendTimer());
        assertNull(appender.getCountersCreatedCounter());
        assertNull(appender.getEventsDroppedCounter());

        // Appending should still work fine
        var event = mockBasicEvent("no obs test");
        appender.append(event);
        assertEquals(1.0, registry.get("logback.to.metrics.no.obs.test.counter").counter().count());
    }

    @Test
    public void testSelfMetricsRegistered() {
        appender.start();

        // Check appender's own fields (avoids flaky global registry interactions between tests)
        assertNotNull(appender.getAppendTimer(), "append timer should be registered");
        assertNotNull(appender.getCountersCreatedCounter(), "counters created counter should be registered");
        assertNotNull(appender.getHistogramsCreatedCounter(), "histograms created counter should be registered");
        assertNotNull(appender.getCardinalityBlacklistedCounter(), "cardinality blacklisted counter should be registered");
        assertNotNull(appender.getReregisterTimer(), "reregister timer should be registered");
        assertNotNull(appender.getEventsDroppedCounter(), "events dropped counter should be registered");

        // Also verify timer and counter types are correct
        assertNotNull(registry.find("logback.to.metrics.appender.append.duration").timer());
        assertNotNull(registry.find("logback.to.metrics.appender.counters.created").counter());
        assertNotNull(registry.find("logback.to.metrics.appender.events.dropped").counter());
    }

    @Test
    public void testSelfMetricsCountersCreatedIncremented() {
        appender.start();

        var event1 = mockBasicEvent("self metric test 1");
        var event2 = mockBasicEvent("self metric test 2");

        appender.append(event1);
        appender.append(event2);

        Counter created = registry.get("logback.to.metrics.appender.counters.created").counter();
        assertEquals(2.0, created.count(), "Should have created 2 counters");
    }

    @Test
    public void testSelfMetricsCardinalityBlacklisted() {
        appender.start();
        appender.setEnableCardinalityProtection(true);
        appender.setMaxTagValueCardinality(3);

        for (int i = 0; i < 5; i++) {
            var event = mockBasicEvent("blacklist metric test");
            when(event.getMDCPropertyMap()).thenReturn(Map.of("highCard", "val_" + i));
            appender.append(event);
        }

        Counter blacklisted = registry.get("logback.to.metrics.appender.cardinality.blacklisted").counter();
        assertEquals(1.0, blacklisted.count(), "Should have blacklisted 1 key");
    }
}
