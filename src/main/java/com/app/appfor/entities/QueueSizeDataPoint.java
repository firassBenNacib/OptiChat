package com.app.appfor.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class QueueSizeDataPoint {
    private final LocalDateTime timestamp;
    private final int queueSize;

    public QueueSizeDataPoint(int queueSize) {
        this.timestamp = LocalDateTime.now();
        this.queueSize = queueSize;
    }


}
