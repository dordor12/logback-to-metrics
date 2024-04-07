# logback-to-metrics

[![Use this template](https://img.shields.io/badge/from-java--library--template-brightgreen?logo=dropbox)](https://github.com/thriving-dev/java-library-template/generate)
[![Java CI](https://github.com/dordor12/logback-to-metrics/actions/workflows/1.pipeline.yml/badge.svg)](https://github.com/dordor12/logback-to-metrics/actions/workflows/1.pipeline.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dordor12/logback-to-metrics.svg)](https://central.sonatype.com/artifact/io.github.dordor12/logback-to-metrics)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://dordor12.github.io/logback-to-metrics/javadoc/)

## Summary
Provides simple integration between your **logs** to your **metrics**.
This integration using logback and micrometer.

## Motivation
A lot of apps logs are consumed via a metric manner, for example:
- How many users buy a product in the past X Hours.
- How many quires to our DB per minute.
- etc..
<br>
That kind of data is usually available in our app logs and might be too heavy to store it for long time.
That's where `logback-to-metrics` come to the rescue, storing metrics is the most efficient way and could be saved fot much longer time!  

## Quick Start
Maven:
```xml
    <groupId>io.github.dordor12</groupId>
    <artifactId>logback-to-metrics</artifactId>
    <version>1.0.0</version>
```
Gradle:
```groovy
    implementation 'io.github.dordor12:logback-to-metrics:1.0.0'
```

To make the appender add the following xml to your app `logback.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    </appender>

<!--  dont forget to add the appender to ref-->
    <root level="INFO">
        <appender-ref ref="LogbackToMetricsAppender" />
    </root>
</configuration>
```

## Configuration

| parameter name      | description                                                                     | default value                                                      |
|---------------------|---------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `maxCounters`       | The max of counters that the appender can create.                               | `10000`                                                            |
| `counterNamePrefix` | The metric name prefix for each metric the appender creates.                    | `logback.to.metrics`                                               |
| `counterNameSubfix` | The metric name prefix for each metric the appender creates.                    | `counter`                                                          |
| `kvWhitelist`       | Whitelist of key-value tags to add to the counter. the kv are created from mdc. | `empty`  - that mean that all keys will be treated as counter tags |
| `kvBlacklist`       | Blacklist of key-value tags to add to the counter. the kv are created from mdc. | `empty`  - that mean that all keys will be treated as counter tags|

To Config one of this parameter you can edit your `logback.xml`, for example:
```xml
<appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    <maxCounters> 10</maxCounters>
    <kvWhitelist>my_key</kvWhitelist>
    <kvWhitelist>bla3</kvWhitelist>
    <kvWhitelist>my_array_key</kvWhitelist>
    <kvBlacklist>bla1</kvBlacklist>
    <kvBlacklist>bla2</kvBlacklist>
    <counterNamePrefix>my.awesome.prefix</counterNamePrefix>
</appender>
```

## Structured logging
This appender supports structured logging via [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder).
to enable all this encoder just add the encoder inside the appender, for example:
```xml
<appender name="LogbackToMetricsAppender" class="io.github.dordor12.LogbackToMetricsAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" >
        <!-- all your logstash encoder configuration -->
    </encoder>>
    <!-- all your logback-to-metrics appender configuration -->
</appender>
```
This integrating with logstash encoder, providers the capability to add a tag to via the [Structured Arguments Api](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#loggingevent-fields) :
```java
@GetMapping("/ping")
public String example() {
    log.info("my log", kv("my_key", "Hello World"), kv("bla", "a"), kv("bla1", "b"));
    log.info("log with array", array("my_array_key", "a1", "a2"));
    return "pong";
}
```
**Note!** The `kvWhitelist` and `kvBlacklist` parameters also effect the key-value pairs from logstash encoder.