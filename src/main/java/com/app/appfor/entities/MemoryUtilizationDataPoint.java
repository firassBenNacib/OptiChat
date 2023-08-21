package com.app.appfor.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemoryUtilizationDataPoint {
    private final long timestamp;
    private final long usedMemory;

    public MemoryUtilizationDataPoint(long timestamp, long usedMemory) {
        this.timestamp = timestamp;
        this.usedMemory = usedMemory;
    }


}
