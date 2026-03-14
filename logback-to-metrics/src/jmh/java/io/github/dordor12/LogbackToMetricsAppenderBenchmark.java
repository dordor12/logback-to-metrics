package io.github.dordor12;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.logstash.logback.marker.Markers;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * JMH benchmarks for LogbackToMetricsAppender using real LoggingEvent objects.
 * <p>
 * Includes a realistic workload of 20 pre-built events with varying characteristics:
 * MDC-only, StructuredArguments, LogstashMarkers, numeric values, high-cardinality
 * tags, mixed sources, and different log levels/loggers.
 * <p>
 * Run with: ./gradlew :logback-to-metrics:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class LogbackToMetricsAppenderBenchmark {

    private static final int EVENT_POOL_SIZE = 20;

    private LogbackToMetricsAppender appender;
    private LogbackToMetricsAppender appenderNoObservability;
    private LogbackToMetricsAppender appenderWithCardinality;
    private LogbackToMetricsAppender appenderWithHistograms;
    private LogbackToMetricsAppender appenderFull;
    private SimpleMeterRegistry registry;
    private Logger logger;
    private Logger apiLogger;
    private Logger dbLogger;
    private Logger kafkaLogger;

    // Pool of 20 realistic events
    private LoggingEvent[] eventPool;

    // Pre-created single events for isolated benchmarks
    private LoggingEvent hotEvent;

    // Rotating counter for round-robin event selection
    private AtomicInteger eventIndex = new AtomicInteger();
    private AtomicInteger coldPathCounter = new AtomicInteger();

    @Setup(org.openjdk.jmh.annotations.Level.Trial)
    public void setup() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);

        LoggerContext loggerContext = new LoggerContext();
        logger = loggerContext.getLogger("com.example.AppService");
        apiLogger = loggerContext.getLogger("com.example.ApiController");
        dbLogger = loggerContext.getLogger("com.example.DatabaseRepo");
        kafkaLogger = loggerContext.getLogger("com.example.KafkaConsumer");

        // --- Appender configs ---

        appender = new LogbackToMetricsAppender();
        appender.setContext(loggerContext);
        appender.start();

        appenderNoObservability = new LogbackToMetricsAppender();
        appenderNoObservability.setEnableSelfObservability(false);
        appenderNoObservability.setContext(loggerContext);
        appenderNoObservability.start();

        appenderWithCardinality = new LogbackToMetricsAppender();
        appenderWithCardinality.setEnableCardinalityProtection(true);
        appenderWithCardinality.setMaxTagValueCardinality(100);
        appenderWithCardinality.setContext(loggerContext);
        appenderWithCardinality.start();

        appenderWithHistograms = new LogbackToMetricsAppender();
        appenderWithHistograms.setEnableAutoHistograms(true);
        appenderWithHistograms.setContext(loggerContext);
        appenderWithHistograms.start();

        // Full-featured: histograms + cardinality + observability
        appenderFull = new LogbackToMetricsAppender();
        appenderFull.setEnableAutoHistograms(true);
        appenderFull.setEnableCardinalityProtection(true);
        appenderFull.setMaxTagValueCardinality(100);
        appenderFull.setContext(loggerContext);
        appenderFull.start();

        // --- Build 20 realistic events ---
        eventPool = new LoggingEvent[EVENT_POOL_SIZE];

        // 0: Simple MDC-only — API request
        eventPool[0] = event(apiLogger, Level.INFO, "Request processed",
                Map.of("endpoint", "/api/users", "method", "GET", "status", "200"));

        // 1: StructuredArguments kv — file upload
        eventPool[1] = eventWithArgs(logger, Level.INFO, "File uploaded successfully",
                Map.of(), kv("file_size_bytes", 8192), kv("processing_time_ms", 47));

        // 2: LogstashMarker — region routing
        var regionMarker = Markers.append("region", "us-east-1").and(Markers.append("env", "prod"));
        eventPool[2] = eventWithMarker(apiLogger, Level.INFO, "Request routed", Map.of(), regionMarker);

        // 3: MDC with numeric values — DB query
        eventPool[3] = event(dbLogger, Level.INFO, "Query executed",
                Map.of("query_duration_ms", "23", "rows_affected", "150", "table", "users"));

        // 4: High-cardinality MDC — unique request ID (bounded pool of 500 values)
        eventPool[4] = event(apiLogger, Level.INFO, "Incoming request",
                Map.of("requestId", "req_00042", "endpoint", "/api/orders"));

        // 5: WARN level with error context
        eventPool[5] = event(dbLogger, Level.WARN, "Slow query detected",
                Map.of("query_duration_ms", "1523", "table", "orders", "threshold_ms", "1000"));

        // 6: Mixed MDC + StructuredArguments
        eventPool[6] = eventWithArgs(logger, Level.INFO, "Payment processed",
                Map.of("currency", "USD", "gateway", "stripe"),
                kv("amount_cents", 4999), kv("retry_count", 0));

        // 7: Kafka consumer — offset tracking (numeric)
        eventPool[7] = event(kafkaLogger, Level.INFO, "Message consumed",
                Map.of("topic", "user-events", "partition", "3", "offset", "892341", "consumer_group", "analytics"));

        // 8: MDC with non-numeric strings only
        eventPool[8] = event(logger, Level.INFO, "User login successful",
                Map.of("auth_method", "oauth2", "provider", "google", "user_role", "admin"));

        // 9: StructuredArguments with string values (no histograms)
        eventPool[9] = eventWithArgs(apiLogger, Level.INFO, "Cache lookup",
                Map.of(), kv("cache_name", "user_sessions"), kv("result", "hit"));

        // 10: LogstashMarker chain — deployment context
        var deployMarker = Markers.append("version", "2.4.1")
                .and(Markers.append("canary", "true"))
                .and(Markers.append("dc", "us-west-2"));
        eventPool[10] = eventWithMarker(logger, Level.INFO, "Service heartbeat", Map.of(), deployMarker);

        // 11: High-cardinality — traceId (unique per event)
        eventPool[11] = event(apiLogger, Level.INFO, "Span completed",
                Map.of("traceId", "abc123def456", "spanId", "span_001", "duration_ms", "89"));

        // 12: ERROR level — exception scenario
        eventPool[12] = event(dbLogger, Level.ERROR, "Connection pool exhausted",
                Map.of("pool_size", "50", "active_connections", "50", "wait_queue", "12"));

        // 13: Mixed marker + MDC + args — full combo
        var auditMarker = Markers.append("audit", "true");
        eventPool[13] = eventWithMarkerAndArgs(logger, Level.INFO, "Data exported",
                Map.of("format", "csv", "rows", "10000"),
                auditMarker, kv("export_duration_ms", 3400), kv("file_size_mb", 24));

        // 14: Simple no-MDC event
        eventPool[14] = event(logger, Level.DEBUG, "Health check passed", Map.of());

        // 15: Numeric-heavy MDC — system metrics
        eventPool[15] = event(logger, Level.INFO, "System metrics collected",
                Map.of("cpu_percent", "67.5", "mem_used_mb", "3421", "gc_pause_ms", "12", "thread_count", "248"));

        // 16: StructuredArguments with double values
        eventPool[16] = eventWithArgs(apiLogger, Level.INFO, "Response latency recorded",
                Map.of("endpoint", "/api/search"),
                kv("p50_ms", 12.5), kv("p99_ms", 187.3));

        // 17: High-cardinality — session ID
        eventPool[17] = event(apiLogger, Level.INFO, "Session validated",
                Map.of("sessionId", "sess_9f8e7d6c", "user_role", "viewer"));

        // 18: Kafka producer — throughput stats
        eventPool[18] = event(kafkaLogger, Level.INFO, "Batch sent",
                Map.of("topic", "click-events", "batch_size", "500", "send_time_ms", "34", "acks", "all"));

        // 19: LogstashMarker single field + MDC
        var featureMarker = Markers.append("feature_flag", "new_checkout");
        eventPool[19] = eventWithMarker(logger, Level.INFO, "Feature gate evaluated",
                Map.of("result", "enabled", "user_segment", "beta"), featureMarker);

        // Pre-warm all appenders with all events so hot path benchmarks hit cache
        for (LoggingEvent e : eventPool) {
            appender.append(e);
            appenderNoObservability.append(e);
            appenderWithCardinality.append(e);
            appenderWithHistograms.append(e);
            appenderFull.append(e);
        }

        hotEvent = eventPool[0];
    }

    @TearDown(org.openjdk.jmh.annotations.Level.Trial)
    public void tearDown() {
        registry.clear();
        Metrics.removeRegistry(registry);
    }

    @Setup(org.openjdk.jmh.annotations.Level.Iteration)
    public void resetCounters() {
        eventIndex.set(0);
        coldPathCounter.set(0);
    }

    // --- Event factory helpers ---

    private LoggingEvent event(Logger log, Level level, String message, Map<String, String> mdc) {
        LoggingEvent e = new LoggingEvent("benchmark", log, level, message, null, null);
        e.setMDCPropertyMap(mdc);
        return e;
    }

    private LoggingEvent eventWithArgs(Logger log, Level level, String message,
                                       Map<String, String> mdc, Object... args) {
        LoggingEvent e = new LoggingEvent("benchmark", log, level, message, null, null);
        e.setMDCPropertyMap(mdc);
        e.setArgumentArray(args);
        return e;
    }

    private LoggingEvent eventWithMarker(Logger log, Level level, String message,
                                         Map<String, String> mdc, org.slf4j.Marker marker) {
        LoggingEvent e = new LoggingEvent("benchmark", log, level, message, null, null);
        e.setMDCPropertyMap(mdc);
        e.setMarker(marker);
        return e;
    }

    private LoggingEvent eventWithMarkerAndArgs(Logger log, Level level, String message,
                                                Map<String, String> mdc, org.slf4j.Marker marker,
                                                Object... args) {
        LoggingEvent e = new LoggingEvent("benchmark", log, level, message, null, null);
        e.setMDCPropertyMap(mdc);
        e.setMarker(marker);
        e.setArgumentArray(args);
        return e;
    }

    // --- Benchmarks ---

    /**
     * Realistic workload: round-robin through 20 pre-warmed events (all cache hits).
     * With self-observability.
     */
    @Benchmark
    public void realisticHotPath() {
        appender.append(eventPool[eventIndex.getAndIncrement() % EVENT_POOL_SIZE]);
    }

    /**
     * Same realistic workload without self-observability overhead.
     */
    @Benchmark
    public void realisticHotPathNoObservability() {
        appenderNoObservability.append(eventPool[eventIndex.getAndIncrement() % EVENT_POOL_SIZE]);
    }

    /**
     * Realistic workload with cardinality protection enabled.
     */
    @Benchmark
    public void realisticHotPathWithCardinality() {
        appenderWithCardinality.append(eventPool[eventIndex.getAndIncrement() % EVENT_POOL_SIZE]);
    }

    /**
     * Realistic workload with auto-histograms enabled (numeric MDC/args recorded).
     */
    @Benchmark
    public void realisticHotPathWithHistograms() {
        appenderWithHistograms.append(eventPool[eventIndex.getAndIncrement() % EVENT_POOL_SIZE]);
    }

    /**
     * Realistic workload with all features: histograms + cardinality + observability.
     */
    @Benchmark
    public void realisticHotPathFullFeatures() {
        appenderFull.append(eventPool[eventIndex.getAndIncrement() % EVENT_POOL_SIZE]);
    }

    /**
     * Single hot event — best-case cache hit (baseline).
     */
    @Benchmark
    public void singleEventHotPath() {
        appender.append(hotEvent);
    }

    /**
     * Single hot event without observability — raw throughput ceiling.
     */
    @Benchmark
    public void singleEventNoObservability() {
        appenderNoObservability.append(hotEvent);
    }

    /**
     * Cold path: unique message each call (cache miss, counter registration).
     */
    @Benchmark
    public void coldPathRegistration() {
        var e = event(logger, Level.INFO, "cold event " + coldPathCounter.getAndIncrement(), Map.of());
        appenderNoObservability.append(e);
    }
}
