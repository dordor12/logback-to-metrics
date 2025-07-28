# Logback to Metrics Demo Application

This demo application showcases the **Auto Histogram Feature** of the `logback-to-metrics` library. It demonstrates how numeric values in your logs are automatically converted into histogram metrics for better observability.

## Features Demonstrated

- **Automatic Histogram Creation**: Numeric values in log statements automatically create histogram metrics
- **Whitelist/Blacklist Filtering**: Control which keys create histograms using configuration
- **Multiple Metric Types**: Response times, file sizes, processing durations, and more
- **Structured Logging**: Uses Logstash encoder with key-value pairs

## Endpoints

### GET `/log`
Basic endpoint that logs response time metrics.

**Example metrics created:**
- `demo.app.metrics.Request.processed.successfully.response_time_ms.histogram`

### GET `/upload/{fileId}`
Simulates file processing with file size and processing duration metrics.

**Example metrics created:**
- `demo.app.metrics.File.processed.successfully.file_size_bytes.histogram`
- `demo.app.metrics.File.processed.successfully.processing_duration.histogram`
- `demo.app.metrics.File.processed.successfully.response_time_ms.histogram`

### POST `/data?size={small|medium|large}`
Processes data of different sizes, creating metrics based on data size.

**Example metrics created:**
- `demo.app.metrics.Data.processing.completed.file_size_bytes.histogram`
- `demo.app.metrics.Data.processing.completed.processing_duration.histogram`

### GET `/metrics/test`
Generates various test metrics to demonstrate the histogram feature.

**Example metrics created:**
- `demo.app.metrics.System.metrics.collected.response_time_ms.histogram`
- `demo.app.metrics.System.metrics.collected.file_size_bytes.histogram`
- `demo.app.metrics.System.metrics.collected.processing_duration.histogram`

Note: The `kafka_offset` value is blacklisted and won't create a histogram.

## Configuration

The application is configured in `logback.xml` with:

- **Histogram Feature Enabled**: `<enableAutoHistograms>true</enableAutoHistograms>`
- **Whitelisted Keys**: `response_time_ms`, `file_size_bytes`, `processing_duration`
- **Blacklisted Keys**: `kafka_offset`
- **Maximum Histograms**: Limited to 2000 to prevent memory issues

## Running the Application

1. Start the application from the root directory:
   ```bash
   ./gradlew :example:bootRun
   ```

2. Test the endpoints:
   ```bash
   curl http://localhost:8080/log
   curl http://localhost:8080/upload/test-file-123
   curl -X POST http://localhost:8080/data?size=medium
   curl http://localhost:8080/metrics/test
   ```

3. Monitor your metrics system to see the automatically created histograms!

## Key Benefits

- **Zero Code Changes**: Just add numeric values to your existing log statements
- **Memory Safe**: Configurable limits prevent OOM issues
- **Selective**: Use whitelist/blacklist to control which metrics are created
- **Standards Compliant**: Works with existing Micrometer and observability tools

## Learn More

- [Main Library Documentation](../README.md)
- [Logback Configuration Guide](https://logback.qos.ch/manual/configuration.html)
- [Micrometer Documentation](https://micrometer.io/docs)