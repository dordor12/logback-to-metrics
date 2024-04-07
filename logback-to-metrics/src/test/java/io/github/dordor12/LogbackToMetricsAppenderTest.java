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
}