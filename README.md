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
    <version>1.0.0</version>
</dependency>
```

### Gradle Configuration
Include this line in your `build.gradle`:
```groovy
implementation 'io.github.dordor12:logback-to-metrics:1.0.0'
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
</appender>
```

## Auto Histogram Feature

The `logback-to-metrics` library can automatically create histograms for numeric values found in your logs. This feature is disabled by default and must be explicitly enabled.

### How It Works

When enabled, the appender scans log events for numeric key-value pairs in:
- **MDC Properties**: Values set via `MDC.put(key, value)`
- **Logstash Encoder JSON**: Key-value pairs in structured log output

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

⚠️ **Default Behavior**: Auto histograms are **disabled by default** to prevent unintended resource usage.

⚠️ **Memory Considerations**: Each histogram consumes memory. Use `maxHistograms` to prevent OOM issues in high-cardinality scenarios.

⚠️ **Performance Impact**: Histogram creation and recording adds processing overhead. Monitor performance in high-throughput scenarios.

## Enhanced Structured Logging
Leverage structured logging with the [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder) for detailed, context-rich logs.

### Configuration Example
Embed the encoder within the `LogbackToMetricsAppender` in your `logback.xml`:
```xml
<appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- Logstash encoder configuration goes here -->
    </encoder>
    <!-- Additional LogbackToMetricsAppender configuration -->
</appender>
```

### Using Structured Arguments API
```java
@GetMapping("/ping")
public String exampleEndpoint() {
    log.info("Processing request", kv("param1", "value1"), kv("param2", "value2"));
    log.info("Request complete", array("metrics", "metric1", "metric2"));
    return "pong";
}
```

The `kvWhitelist` and `kvBlacklist` settings also apply to key-value pairs specified through the Logstash encoder, ensuring consistent tag filtering across your logs and metrics.

Similarly, the `histogramKvWhitelist` and `histogramKvBlacklist` settings control which numeric values from the Logstash encoder are used for histogram creation.

## Example

See the [example project](example/README.md) for a complete demonstration of the library's features, including automatic histogram creation and structured logging integration.
