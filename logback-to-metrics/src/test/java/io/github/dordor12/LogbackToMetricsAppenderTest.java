package io.github.dordor12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Metrics;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


import java.util.List;
import java.util.Map;

public class LogbackToMetricsAppenderTest {

    private LogbackToMetricsAppender appender;
    private SimpleMeterRegistry registry;
    private LogstashEncoder logstashEncoder;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        logstashEncoder = Mockito.mock(LogstashEncoder.class);
        appender = Mockito.spy(new LogbackToMetricsAppender());
        when(appender.getEncoder()).thenReturn(logstashEncoder);
        when(logstashEncoder.encode(any())).thenReturn(new ObjectMapper().writeValueAsBytes("""
        {}
        """));

        // Enable auto histograms for testing
        appender.setEnableAutoHistograms(true);

        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry); // Make sure to clean this up if necessary in an @After method
    }
    @AfterEach
    public void afterEach() {
        registry.clear();
        Metrics.removeRegistry(registry);
    }

    private void mockBasicEvent(ILoggingEvent event, String msg) {
        when(event.getMessage()).thenReturn(msg);
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("IncrementLogger");
        when(event.getThreadName()).thenReturn("IncrementThread");
    }

    @Test
    public void testCounterIncrement() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "basic counter event");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        assertEquals(2.0, registry.get("logback.to.metrics.basic.counter.event.counter").counter().count());
    }

    @Test
    public void testCounterIncrementCustomName() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "basic counter event rename");
        appender.setCounterNamePrefix("custom.name");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        assertEquals(2.0, registry.get("custom.name.basic.counter.event.rename.counter").counter().count());
    }

    @Test
    public void testMaxCounters() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "my event");
        var event1 = mock(ILoggingEvent.class);
        mockBasicEvent(event1, "Increment Test1");
        var event2 = mock(ILoggingEvent.class);
        mockBasicEvent(event2, "Increment Test2");
        appender.setMaxCounters(2L);

        appender.append(event);
        appender.append(event1);
        appender.append(event2);
        appender.append(event); // Increment twice for the same event properties

        assertEquals(2.0, registry.get("logback.to.metrics.my.event.counter").counter().count());
        assertEquals( 1.0, registry.get("logback.to.metrics.Increment.Test1.counter").counter().count());
        assertNull(registry.find("logback.to.metrics.Increment.Test2.counter").counter());

    }

    @Test
    public void testMaxCountersWhenTagsChange() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "basic event");
        var event1 = mock(ILoggingEvent.class);
        mockBasicEvent(event1, "Increment Test1");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"))
                .thenReturn(Map.of("key1", "value2"));
        appender.setMaxCounters(2L);

        appender.append(event);
        appender.append(event1);
        appender.append(event); // Increment twice for the same event properties

        assertEquals( 1.0, registry.get("logback.to.metrics.basic.event.counter").counter().count());
        assertEquals( 1.0, registry.get("logback.to.metrics.Increment.Test1.counter").counter().count());
        assertNull(registry.find("logback.to.metrics.Increment.Test2.counter").counter());
    }

    @Test
    public void testAddTags() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "basic event tags");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        assertEquals( 2.0, registry.get("logback.to.metrics.basic.event.tags.counter").counter().count());
        List<Tag> tags = registry.get("logback.to.metrics.basic.event.tags.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenNotInWhiteListShouldNotAdded() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "white and black list1");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));
        appender.addKvWhitelist("bla");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list1.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenInWhiteListShouldAdded() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "white and black list2");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));
        appender.addKvWhitelist("key1");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list2.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenNotInBlackListListShouldAdded() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "white and black list3");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("bla");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list3.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenInBlackListListShouldNotAdded() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "white and black list4");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("key1");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list4.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testAddTagsWhenInBlackListAndInWhitelistListShouldNotAdded() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "white and black list5");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("key1", "value1"));
        appender.addKvBlacklist("key1");
        appender.addKvWhitelist("key1");

        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.white.and.black.list5.counter").meter().getId().getTags();
        assertFalse(tags.stream().anyMatch(a -> a.getKey().equals("key1") && a.getValue().equals("value1")));
    }

    @Test
    public void testLogstashEncoderIntegration() throws JsonProcessingException {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "logstash encoder");
        when(logstashEncoder.encode(event)).thenReturn(new ObjectMapper().writeValueAsBytes(Map.of("json_key", "val")));
        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.logstash.encoder.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("json_key") && a.getValue().equals("val")));
    }

    @Test
    public void testLogstashEncoderIntegrationWhenKeyIsList() throws JsonProcessingException {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "logstash encoder list");
        when(logstashEncoder.encode(event)).thenReturn(new ObjectMapper().writeValueAsBytes(Map.of("json_key", List.of("a","b"))));
        appender.append(event);
        appender.append(event); // Increment twice for the same event properties

        List<Tag> tags = registry.get("logback.to.metrics.logstash.encoder.list.counter").meter().getId().getTags();
        assertTrue(tags.stream().anyMatch(a -> a.getKey().equals("json_key") && a.getValue().equals("a,b")));
    }

    // Histogram tests
    @Test
    public void testHistogramCreatedForNumericValueInMDC() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "numeric test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("response_time", "123"));

        appender.append(event);
        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.numeric.test.response_time.histogram").summary();
        assertEquals(2, histogram.count());
        assertEquals(246.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramCreatedForDoubleValue() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "double test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("cpu_usage", "45.67"));

        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.double.test.cpu_usage.histogram").summary();
        assertEquals(1, histogram.count());
        assertEquals(45.67, histogram.totalAmount(), 0.01);
    }

    @Test
    public void testHistogramNotCreatedForNonNumericValue() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "non numeric test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("user_name", "john_doe"));

        appender.append(event);

        assertNull(registry.find("logback.to.metrics.non.numeric.test.user_name.histogram").summary());
    }

    @Test
    public void testHistogramCreatedFromLogstashEncoder() throws JsonProcessingException {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "logstash numeric");
        when(logstashEncoder.encode(event)).thenReturn(new ObjectMapper().writeValueAsBytes(Map.of("duration", "500")));

        appender.append(event);

        DistributionSummary histogram = registry.get("logback.to.metrics.logstash.numeric.duration.histogram").summary();
        assertEquals(1, histogram.count());
        assertEquals(500.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramWhitelistFiltering() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "whitelist test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("allowed_metric", "100", "blocked_metric", "200"));
        
        appender.addHistogramKvWhitelist("allowed_metric");

        appender.append(event);

        assertNotNull(registry.find("logback.to.metrics.whitelist.test.allowed_metric.histogram").summary());
        assertNull(registry.find("logback.to.metrics.whitelist.test.blocked_metric.histogram").summary());
    }

    @Test
    public void testHistogramBlacklistFiltering() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "blacklist test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("allowed_metric", "100", "blocked_metric", "200"));
        
        appender.addHistogramKvBlacklist("blocked_metric");

        appender.append(event);

        assertNotNull(registry.find("logback.to.metrics.blacklist.test.allowed_metric.histogram").summary());
        assertNull(registry.find("logback.to.metrics.blacklist.test.blocked_metric.histogram").summary());
    }

    @Test
    public void testHistogramBlacklistOverridesWhitelist() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "override test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of("conflicted_metric", "100"));
        
        appender.addHistogramKvWhitelist("conflicted_metric");
        appender.addHistogramKvBlacklist("conflicted_metric");

        appender.append(event);

        assertNull(registry.find("logback.to.metrics.override.test.conflicted_metric.histogram").summary());
    }

    @Test
    public void testMaxHistogramsLimit() {
        appender.setMaxHistograms(2L);
        
        var event1 = mock(ILoggingEvent.class);
        mockBasicEvent(event1, "limit test 1");
        when(event1.getMDCPropertyMap()).thenReturn(Map.of("metric1", "100"));
        
        var event2 = mock(ILoggingEvent.class);
        mockBasicEvent(event2, "limit test 2");
        when(event2.getMDCPropertyMap()).thenReturn(Map.of("metric2", "200"));
        
        var event3 = mock(ILoggingEvent.class);
        mockBasicEvent(event3, "limit test 3");
        when(event3.getMDCPropertyMap()).thenReturn(Map.of("metric3", "300"));

        appender.append(event1);
        appender.append(event2);
        appender.append(event3); // This should be ignored due to limit

        assertNotNull(registry.find("logback.to.metrics.limit.test.1.metric1.histogram").summary());
        assertNotNull(registry.find("logback.to.metrics.limit.test.2.metric2.histogram").summary());
        assertNull(registry.find("logback.to.metrics.limit.test.3.metric3.histogram").summary());
    }

    @Test
    public void testMaxHistogramsLimitPreventOOM() {
        // Clear registry at start and create a simple test that checks the limit is enforced
        registry.clear();
        appender.setEnableAutoHistograms(true); // Enable histograms for this test
        appender.setMaxHistograms(3L); // Use small number for fast, reliable test
        appender.setMaxCounters(0L); // Disable counters to isolate histogram count
        
        // Create events that should create 5 different histograms, but only 3 should be created due to limit
        for (int i = 0; i < 5; i++) {
            var event = mock(ILoggingEvent.class);
            mockBasicEvent(event, "limit test " + i);
            when(event.getMDCPropertyMap()).thenReturn(Map.of("metric" + i, String.valueOf(i * 10)));
            appender.append(event);
        }

        // Should only have created maxHistograms (3) histograms
        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();
        
        assertTrue(histogramCount <= 3L, "Should have at most 3 histograms, but got " + histogramCount);
    }

    @Test
    public void testHistogramWithDifferentNumericTypes() {
        // Test each numeric type separately to avoid issues with mock
        testNumericType("int_val", "42", 42.0);
        testNumericType("long_val", "9876543210", 9876543210.0);
        testNumericType("double_val", "3.14159", 3.14159);
        // Note: Negative values are not recorded in DistributionSummary as they represent invalid measurements
        testNegativeNumericType("negative_int", "-100");
        testNegativeNumericType("negative_double", "-2.5");
    }

    private void testNumericType(String key, String value, double expectedValue) {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "types test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(key, value));

        appender.append(event);

        DistributionSummary hist = registry.get("logback.to.metrics.types.test." + key + ".histogram").summary();
        assertEquals(1, hist.count(), "Count for " + key);
        assertEquals(expectedValue, hist.totalAmount(), 0.00001, "Total amount for " + key);
        
        // Clear registry for next test
        registry.clear();
    }

    private void testNegativeNumericType(String key, String value) {
        var event = mock(ILoggingEvent.class);  
        mockBasicEvent(event, "types test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of(key, value));

        appender.append(event);

        // Histogram should be created but no values should be recorded (DistributionSummary ignores negative values)
        DistributionSummary hist = registry.get("logback.to.metrics.types.test." + key + ".histogram").summary();
        assertEquals(0, hist.count(), "Count for negative " + key + " should be 0");
        assertEquals(0.0, hist.totalAmount(), 0.00001, "Total amount for negative " + key + " should be 0");
        
        // Clear registry for next test
        registry.clear();
    }

    @Test
    public void testHistogramIgnoresEmptyAndNullValues() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "empty test");
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of(
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
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "simple test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        DistributionSummary histogram = registry.find("logback.to.metrics.simple.test.test_metric.histogram").summary();
        assertNotNull(histogram);
        System.out.println("Histogram count: " + histogram.count());
        System.out.println("Histogram total: " + histogram.totalAmount());
        assertEquals(1, histogram.count());
        assertEquals(42.0, histogram.totalAmount());
    }

    @Test 
    public void testNegativeHistogramCreation() {
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "negative test");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("negative_metric", "-100"));

        appender.append(event);

        DistributionSummary histogram = registry.find("logback.to.metrics.negative.test.negative_metric.histogram").summary();
        assertNotNull(histogram);
        System.out.println("Negative histogram count: " + histogram.count());
        System.out.println("Negative histogram total: " + histogram.totalAmount());
        // DistributionSummary ignores negative values, so count should be 0
        assertEquals(0, histogram.count());
        assertEquals(0.0, histogram.totalAmount());
    }

    @Test
    public void testHistogramToggleDisabled() {
        // Clear registry and test that histograms are not created when the feature is disabled
        registry.clear();
        appender.setEnableAutoHistograms(false); // Explicitly disable
        
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "toggle test disabled");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        // Should not create any histograms when disabled
        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();
        
        assertEquals(0L, histogramCount);
    }

    @Test
    public void testHistogramToggleEnabled() {
        // Clear registry and test that histograms are created when the feature is enabled
        registry.clear();
        appender.setEnableAutoHistograms(true); // Explicitly enable
        
        var event = mock(ILoggingEvent.class);
        mockBasicEvent(event, "toggle test enabled");
        when(event.getMDCPropertyMap()).thenReturn(Map.of("test_metric", "42"));

        appender.append(event);

        // Should create histograms when enabled
        long histogramCount = registry.getMeters().stream()
                .filter(meter -> meter instanceof DistributionSummary)
                .count();
        
        assertEquals(1L, histogramCount);
    }
}