package com.app.appfor.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemoryUtilizationDataPoint {
    private LocalDateTime timestamp;
    private final long usedMemory;

    public MemoryUtilizationDataPoint(long usedMemory) {
        this.timestamp = LocalDateTime.now();
        this.usedMemory = usedMemory;
    }


}
