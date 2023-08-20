package com.app.appfor.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MessageSizeDataPoint {
    private final LocalDateTime timestamp;
    private final long messageSizeBytes;

    public MessageSizeDataPoint(long messageSizeBytes) {
        this.timestamp = LocalDateTime.now();
        this.messageSizeBytes = messageSizeBytes;
    }
}
