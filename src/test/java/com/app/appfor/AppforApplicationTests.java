package com.app.appfor;

import com.app.appfor.controller.MetricsController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class AppforApplicationTests {

	private MetricsController metricsController;

	@BeforeEach
	void setUp() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		metricsController = new MetricsController(meterRegistry);
	}

	@Test
	void trackTraffic_shouldIncrementTrafficCount() {
		metricsController.trackTraffic();
		double trafficCount = metricsController.getTrafficCount();
		assertEquals(1, trafficCount);
	}

	@Test
	void decrementTraffic_shouldDecrementTrafficCount() {
		metricsController.trackTraffic();
		metricsController.decrementTrafficmanual();
		double trafficCount = metricsController.getTrafficCount();
		assertEquals(0, trafficCount);
	}

	// Add more test methods to cover other functionality of MetricsController
}