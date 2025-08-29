package com.orchestrator.core.controller;

import com.orchestrator.core.metrics.LatencyTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    
    private final LatencyTracker latencyTracker;
    
    public MetricsController(LatencyTracker latencyTracker) {
        this.latencyTracker = latencyTracker;
    }
    
    @GetMapping("/latency")
    public Map<String, Object> getLatencyMetrics() {
        long totalMessages = latencyTracker.getTotalMessageCount();
        long slowMessages = latencyTracker.getSlowMessageCount();
        double slowPercentage = latencyTracker.getSlowMessagePercentage();
        
        // Log periodic stats when endpoint is called
        latencyTracker.logPeriodicStats();
        
        return Map.of(
            "totalMessages", totalMessages,
            "slowMessages", slowMessages,
            "slowPercentage", String.format("%.2f%%", slowPercentage),
            "fastMessages", totalMessages - slowMessages,
            "latencyThreshold", "1000ms",
            "timestamp", System.currentTimeMillis()
        );
    }
    
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return Map.of(
            "status", "running",
            "partitions", 20,
            "targetTPS", 1000,
            "latencyThreshold", "1 second",
            "databaseStrategy", "optimized for high throughput"
        );
    }
}