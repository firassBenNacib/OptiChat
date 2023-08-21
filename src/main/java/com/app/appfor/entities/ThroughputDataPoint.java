package com.app.appfor.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ThroughputDataPoint {
    private final long timestamp;
    private final double throughput;

}
