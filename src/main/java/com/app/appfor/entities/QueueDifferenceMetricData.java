package com.app.appfor.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class QueueDifferenceMetricData {
    private final LocalDateTime timestamp;
    private final int metric;

    public QueueDifferenceMetricData(int metric) {
        this.timestamp = LocalDateTime.now();
        this.metric = metric;

    }
}