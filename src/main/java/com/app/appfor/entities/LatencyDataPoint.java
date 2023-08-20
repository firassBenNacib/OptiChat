package com.app.appfor.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class LatencyDataPoint {
    private final LocalDateTime timestamp;
    private final long latencyMillis;

    public LatencyDataPoint(long latencyMillis) {
        this.timestamp = LocalDateTime.now();
        this.latencyMillis = latencyMillis;
    }
}
