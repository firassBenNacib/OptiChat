package com.app.appfor.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MergedDataEntry {
    private LocalDateTime timestamp;
    private String message;
    private String messageNumber;
    private int queueSize;
    private int queueDifferenceMetric;
    private long latencyMillis;
    private long messageSizeBytes;
    private double throughput;
    private long usedMemory;


}
