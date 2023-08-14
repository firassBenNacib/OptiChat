package com.app.appfor.entities;


import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
public class ProcessedMessage {

    private final LocalDateTime timestamp;
    private final String content;

    public ProcessedMessage(String content) {
        this.timestamp = LocalDateTime.now();
        this.content = content;
    }

}
