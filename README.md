# Logback to Metrics Integration

[![Use this template](https://img.shields.io/badge/from-java--library--template-brightgreen?logo=dropbox)](https://github.com/thriving-dev/java-library-template/generate)
[![Java CI](https://github.com/dordor12/logback-to-metrics/actions/workflows/1.pipeline.yml/badge.svg)](https://github.com/dordor12/logback-to-metrics/actions/workflows/1.pipeline.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dordor12/logback-to-metrics.svg)](https://central.sonatype.com/artifact/io.github.dordor12/logback-to-metrics)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://dordor12.github.io/logback-to-metrics/javadoc/)

## Overview
The `logback-to-metrics` library seamlessly integrates logging with metrics collection, utilizing Logback and Micrometer to enhance observability and performance monitoring in Java applications.

## Why Use Logback to Metrics?
In modern applications, extracting actionable insights and performance metrics from logs is crucial for monitoring and analyzing:

- User interactions and transactions over time.
- Database query frequencies and patterns.
- System-wide operation rates and errors.

Logs often contain this valuable data, yet storing logs for long-term analysis can be resource-intensive. `logback-to-metrics` offers an efficient solution by converting log data into metrics, enabling longer retention periods and more accessible analysis.

## Getting Started
### Maven Configuration
Add the following dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>io.github.dordor12</groupId>
    <artifactId>logback-to-metrics</artifactId>
    <version>0.2.1-SNAPSHOT</version>
</dependency>
```

### Gradle Configuration
Include this line in your `build.gradle`:
```groovy
implementation 'io.github.dordor12:logback-to-metrics:0.2.1-SNAPSHOT'
```

### Integrating with Logback
Modify your application's `logback.xml` to include the `LogbackToMetricsAppender`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    </appender>

    <!-- Reference the new appender -->
    <root level="INFO">
        <appender-ref ref="LogbackToMetricsAppender" />
    </root>
</configuration>
```

## Configuration Options

Customize the `LogbackToMetricsAppender` using the following parameters:

### Counter Configuration
| Parameter            | Description                                                                   | Default Value                                                     |
|----------------------|-------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `maxCounters`        | Maximum number of counters the appender can create.                          | `10000`                                                           |
| `counterNamePrefix`  | Prefix for each metric name created by the appender.                         | `logback.to.metrics`                                              |
| `counterNameSubfix`  | Suffix for each metric name created by the appender.                         | `counter`                                                         |
| `kvWhitelist`        | Whitelist of MDC key-value tags to include as counter tags.                  | (None) All keys are included by default.                          |
| `kvBlacklist`        | Blacklist of MDC key-value tags to exclude from counter tags.                | (None) No keys are excluded by default.                           |

### Histogram Configuration
| Parameter                | Description                                                                   | Default Value                                                     |
|--------------------------|-------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `enableAutoHistograms`   | Enable/disable automatic histogram creation for numeric values.              | `false`                                                           |
| `maxHistograms`          | Maximum number of histograms the appender can create.                        | `10000`                                                           |
| `histogramKvWhitelist`   | Whitelist of keys to consider for histogram creation.                        | (None) All keys are considered by default.                        |
| `histogramKvBlacklist`   | Blacklist of keys to exclude from histogram creation.                        | (None) No keys are excluded by default.                           |
| `histogramNameSubfix`    | Suffix for histogram metric names.                                           | `histogram`                                                       |

### Cardinality Protection
| Parameter                       | Description                                                           | Default Value |
|---------------------------------|-----------------------------------------------------------------------|---------------|
| `enableCardinalityProtection`   | Auto-detect and blacklist high-cardinality tag keys at runtime.      | `false`       |
| `maxTagValueCardinality`        | Max distinct values per tag key before auto-blacklisting.            | `100`         |

### Self-Observability
| Parameter                    | Description                                                              | Default Value |
|------------------------------|--------------------------------------------------------------------------|---------------|
| `enableSelfObservability`    | Register internal metrics for monitoring appender health and performance. | `true`        |

When enabled, the appender tracks the number of distinct values for each tag key. If a key exceeds `maxTagValueCardinality`, it is automatically blacklisted: existing counters containing that tag are removed from the Micrometer registry and re-registered without the offending tag, preventing unbounded series in metrics backends.

Histograms are excluded from cardinality protection — only counter tags are tracked and cleaned up.

Example `logback.xml` configuration:
```xml
<appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    <!-- Counter Configuration -->
    <maxCounters>10</maxCounters>
    <kvWhitelist>key1</kvWhitelist>
    <kvWhitelist>key2</kvWhitelist>
    <kvBlacklist>key3</kvBlacklist>
    <counterNamePrefix>my.app.metrics</counterNamePrefix>

    <!-- Histogram Configuration -->
    <enableAutoHistograms>true</enableAutoHistograms>
    <maxHistograms>5000</maxHistograms>
    <histogramKvWhitelist>file_size</histogramKvWhitelist>
    <histogramKvBlacklist>kafka_offset</histogramKvBlacklist>

    <!-- Cardinality Protection -->
    <enableCardinalityProtection>true</enableCardinalityProtection>
    <maxTagValueCardinality>50</maxTagValueCardinality>

    <!-- Self-Observability (enabled by default, disable for max throughput) -->
    <enableSelfObservability>false</enableSelfObservability>
</appender>
```

## Auto Histogram Feature

The `logback-to-metrics` library can automatically create histograms for numeric values found in your logs. This feature is disabled by default and must be explicitly enabled.

### How It Works

When enabled, the appender scans log events for numeric key-value pairs in:
- **MDC Properties**: Values set via `MDC.put(key, value)`
- **StructuredArguments**: Values from `kv("key", numericValue)`
- **LogstashMarkers**: Values from `Markers.append("key", numericValue)`

For any numeric value (int, Integer, long, Long, double, Double), a histogram is automatically created with the format:
```
{counterNamePrefix}.{log_message}.{key}.{histogramNameSubfix}
```

### Example Usage

```java
@GetMapping("/upload")
public String uploadFile(@RequestParam("file") MultipartFile file) {
    long processingTime = System.currentTimeMillis();

    // Process file...

    processingTime = System.currentTimeMillis() - processingTime;

    // These numeric values will automatically create histograms
    log.info("File uploaded successfully",
        kv("file_size", file.getSize()),
        kv("processing_time_ms", processingTime));

    return "success";
}
```

This would create histograms:
- `logback.to.metrics.File.uploaded.successfully.file_size.histogram`
- `logback.to.metrics.File.uploaded.successfully.processing_time_ms.histogram`

### Key Features

- **Memory Safe**: Configurable maximum histogram limit prevents OOM issues
- **Selective Creation**: Use whitelist/blacklist to control which keys create histograms
- **Type Detection**: Automatically handles various numeric types
- **Negative Value Handling**: Negative values are ignored (as per Micrometer DistributionSummary design)
- **Toggle Control**: Can be enabled/disabled via configuration

### Important Notes

**Default Behavior**: Auto histograms are **disabled by default** to prevent unintended resource usage.

**Memory Considerations**: Each histogram consumes memory. Use `maxHistograms` to prevent OOM issues in high-cardinality scenarios.

**Performance Impact**: Histogram creation and recording adds processing overhead. Monitor performance in high-throughput scenarios.

## Self-Observability Metrics

The appender registers internal metrics (prefixed with `logback.to.metrics.appender`) for monitoring its own health and performance:

| Metric | Type | Description |
|--------|------|-------------|
| `appender.append.duration` | Timer | Time spent in `append()` per event |
| `appender.counters.created` | Counter | Total number of counters registered |
| `appender.histograms.created` | Counter | Total number of histograms registered |
| `appender.counters.active` | Gauge | Current number of active counters |
| `appender.histograms.active` | Gauge | Current number of active histograms |
| `appender.cardinality.blacklisted` | Counter | Number of tag keys auto-blacklisted |
| `appender.cardinality.reregister.duration` | Timer | Time spent in counter re-registration |
| `appender.counters.saturated` | Gauge | 1 if counter circuit breaker tripped, 0 otherwise |
| `appender.events.dropped` | Counter | Events skipped due to circuit breaker |

These metrics are registered when `start()` is called (automatically by Logback during appender initialization). Set `enableSelfObservability` to `false` to disable all internal metrics for maximum throughput (~70% improvement in benchmarks).

## Structured Arguments & LogstashMarkers

The appender extracts key-value pairs directly from [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder) StructuredArguments and LogstashMarkers — no encoder configuration required.

### StructuredArguments
```java
import static net.logstash.logback.argument.StructuredArguments.kv;

log.info("Processing request", kv("endpoint", "/api/v1"), kv("user_id", "u123"));
```

### LogstashMarkers
```java
import static net.logstash.logback.marker.Markers.append;

log.info(append("region", "us-east-1").and(append("env", "prod")), "Request routed");
```

Both StructuredArguments and LogstashMarkers are extracted as metric tags alongside MDC properties. The `kvWhitelist` and `kvBlacklist` settings apply uniformly to all tag sources (MDC, StructuredArguments, LogstashMarkers).

Similarly, the `histogramKvWhitelist` and `histogramKvBlacklist` settings control which numeric values from any source are used for histogram creation.

## Performance

The appender is designed for high-throughput logging pipelines. It extracts data directly from event objects (no JSON round-tripping), uses `CacheKey`-based lookups for zero-allocation hot paths, and includes circuit breakers when metric limits are reached.

### Benchmark Results

Measured with JMH using 20 realistic `LoggingEvent` objects (MDC tags, StructuredArguments, LogstashMarkers, numeric values, mixed levels/loggers) on an Apple M-series chip, single thread:

| Scenario | Throughput |
|----------|-----------|
| Single event hot path (no observability) | **55.1M ops/s** |
| Cold path — new counter registration | **40.7M ops/s** |
| Single event hot path (with observability) | **17.5M ops/s** |
| Realistic 20-event mix (no observability) | **15.9M ops/s** |
| Realistic 20-event mix (with observability) | **9.2M ops/s** |
| Realistic + cardinality protection | **8.1M ops/s** |
| Realistic + histograms + cardinality + observability | **2.5M ops/s** |

### Comparison with Logging Frameworks

| Framework / Appender | Throughput | Source |
|---------------------|-----------|--------|
| **logback-to-metrics** realistic hot path | **15.9M ops/s** | This project |
| **logback-to-metrics** full features | **2.5M ops/s** | This project |
| Logback noop appender | 12–42M ops/s | [logback.qos.ch](https://logback.qos.ch/performance.html) |
| Logback sync file appender | ~1.8M ops/s | [Terse Systems](https://tersesystems.com/blog/2019/06/03/application-logging-in-java-part-6/) |
| Logback async disruptor | ~3.7M ops/s | [Terse Systems](https://tersesystems.com/blog/2019/06/03/application-logging-in-java-part-6/) |
| Logback sync (official) | ~2.2M ops/s | [logback.qos.ch](https://logback.qos.ch/performance.html) |
| Log4j2 async file | ~903K ops/s | [OpenElements](https://github.com/OpenElements/java-logger-benchmark) |
| Chronicle Logger async | ~2.2M ops/s | [OpenElements](https://github.com/OpenElements/java-logger-benchmark) |

Even with all features enabled (histograms + cardinality protection + self-observability), the appender matches Logback's own sync throughput and outperforms most file-writing appenders. In practice, disk/network I/O in other appenders in your pipeline will always be the bottleneck — not metric extraction.

### Live Benchmark Dashboard

Benchmark results are tracked across commits and published automatically via CI:

[![JMH Benchmark](https://img.shields.io/badge/JMH-Benchmark%20Dashboard-blue)](https://dordor12.github.io/logback-to-metrics/dev/bench/)

### Run Benchmarks Locally

```bash
./gradlew :logback-to-metrics:jmh
```

## Example

See the [example project](example/README.md) for a complete demonstration of the library's features, including automatic histogram creation, cardinality protection, and structured logging integration.
