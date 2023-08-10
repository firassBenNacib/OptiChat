package com.app.appfor;


import com.app.appfor.controller.MetricsController;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsControllerTest {

    private MetricsController metricsController;

    @BeforeEach
    public void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        metricsController = new MetricsController(meterRegistry);
    }

    @Test
    public void testTrackTraffic() {
        String response = metricsController.trackTraffic();
        assertEquals("Traffic tracked = 1.0", response);
    }

    @Test
    public void testDecrementTrafficManual() {
        metricsController.decrementTrafficmanual();
        String response = metricsController.getTrafficGaugeValue();
        assertEquals("0.0", response);
    }


}
