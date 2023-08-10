package com.app.appfor.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class MetricsController {
    private final Gauge trafficGauge;
    private double trafficCount = 0;

    private final Counter httpRequestsCounter;

    @Autowired
    public MetricsController(MeterRegistry meterRegistry) {
        this.trafficGauge = Gauge.builder("myapp_traffic_gauge", () -> trafficCount)
                .register(meterRegistry);
        this.httpRequestsCounter = Counter.builder("myapp_http_requests_counter")
                .description("Number of HTTP requests to /hello")
                .register(meterRegistry);
    }

    @GetMapping("/traffic")
    public String trackTraffic() {
        incrementTrafficCount();
        return "Traffic tracked = " + getTrafficCount();
    }

    @Scheduled(fixedDelay = 30000) // Runs every 30 seconds
    public void decrementTraffic() {
        decreaseTrafficCount();
    }

    @GetMapping("/decrement")
    public String decrementTrafficmanual() {
        decreaseTrafficCount();
        return "Traffic tracked = " + getTrafficCount();
    }

    @GetMapping("/traffic-gauge")
    public String getTrafficGaugeValue() {
        return Double.toString(trafficGauge.value());
    }

    private void incrementTrafficCount() {
        trafficCount++;
    }

    private void decreaseTrafficCount() {
        if (trafficCount > 0) {
            trafficCount--;
        }
    }

    public double getTrafficCount() {
        return trafficCount;
    }

    @GetMapping("/hello")
    public String hello() {
        httpRequestsCounter.increment();
        return "Hello";
    }
}
