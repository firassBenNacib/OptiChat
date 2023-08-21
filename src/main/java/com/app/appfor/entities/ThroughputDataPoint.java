package com.app.appfor.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ThroughputDataPoint {
    private final LocalDateTime timestamp;
    private final double throughput;

}
