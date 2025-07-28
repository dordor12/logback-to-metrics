package io.github.dordor12.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Random;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@Slf4j
public class DemoController {

    private final Random random = new Random();

    @GetMapping("/log")
    public Mono<String> logMessage() {
        long startTime = System.currentTimeMillis();
        
        // Simulate some processing time
        long responseTime = 15 + random.nextInt(85); // 15-100ms
        
        log.info("Request processed successfully", 
            kv("response_time_ms", responseTime),
            kv("endpoint", "/log"),
            kv("user_id", "user_" + random.nextInt(1000)));
            
        return Mono.just("Log message sent with histogram metrics!");
    }

    @GetMapping("/upload/{fileId}")
    public Mono<String> simulateFileProcess(@PathVariable String fileId) {
        // Simulate file processing with random file sizes and processing times
        long fileSizeBytes = 1024 + random.nextInt(10240); // 1KB to 11KB
        long processingDuration = random.nextInt(200) + 50; // 50-250ms
        long responseTime = processingDuration + random.nextInt(20);
        
        log.info("File processed successfully",
            kv("file_size_bytes", fileSizeBytes),
            kv("processing_duration", processingDuration),
            kv("response_time_ms", responseTime),
            kv("endpoint", "/upload"),
            kv("file_id", fileId));
            
        return Mono.just("File " + fileId + " processed with metrics!");
    }

    @PostMapping("/data")
    public Mono<String> processData(@RequestParam(defaultValue = "small") String size) {
        // Simulate different data processing sizes
        long dataSize;
        switch (size.toLowerCase()) {
            case "small":
                dataSize = 100 + random.nextInt(500);
                break;
            case "medium":
                dataSize = 1000 + random.nextInt(5000);
                break;
            case "large":
                dataSize = 10000 + random.nextInt(50000);
                break;
            default:
                dataSize = 100;
                break;
        }
        
        // Processing time correlates with data size
        long processingTime = dataSize / 10 + random.nextInt(50);
        long responseTime = processingTime + random.nextInt(30);
        
        log.info("Data processing completed",
            kv("file_size_bytes", dataSize),
            kv("processing_duration", processingTime),
            kv("response_time_ms", responseTime),
            kv("data_type", size),
            kv("endpoint", "/data"));
            
        return Mono.just("Data processed: " + dataSize + " bytes");
    }

    @GetMapping("/metrics/test")
    public Mono<String> generateTestMetrics() {
        // Generate various numeric metrics to demonstrate histogram creation
        long responseTime = 25 + random.nextInt(200);
        long fileSize = 2048 + random.nextInt(8192);
        double cpuUsage = 10.0 + (random.nextDouble() * 80.0);
        int activeConnections = 5 + random.nextInt(95);
        
        log.info("System metrics collected",
            kv("response_time_ms", responseTime),
            kv("file_size_bytes", fileSize),
            kv("processing_duration", responseTime - 5),
            kv("cpu_usage_percent", String.format("%.2f", cpuUsage)),
            kv("active_connections", activeConnections),
            kv("endpoint", "/metrics/test"));
            
        // This demonstrates a value that will be blacklisted
        log.info("Kafka message processed",
            kv("kafka_offset", 12345678L), // This will be blacklisted
            kv("response_time_ms", 15)); // This will create histogram
            
        return Mono.just("Test metrics generated successfully!");
    }
}