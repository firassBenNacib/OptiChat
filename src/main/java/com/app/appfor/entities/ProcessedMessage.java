package com.app.appfor.entities;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ProcessedMessage {

    private final LocalDateTime timestamp;
    private final String message;
    private final String messageNumber;

    public ProcessedMessage(String content) {
        this.timestamp = LocalDateTime.now();
        String[] parts = content.split(" ");
        if (parts.length >= 2) {
            this.message = parts[0];
            this.messageNumber = parts[1];
        } else {
            this.message = content;
            this.messageNumber = "";
        }
    }
}