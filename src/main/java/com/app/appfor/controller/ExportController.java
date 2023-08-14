package com.app.appfor.controller;

import com.app.appfor.Component.MessageReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final MessageReceiver messageReceiver;

    @Autowired
    public ExportController(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    @GetMapping("/processed-messages")
    public ResponseEntity<String> exportProcessedMessages() {
        messageReceiver.exportProcessedMessagesToCSV("C:/Users/MSI/OneDrive/Bureau/processed_messages.csv");
        return ResponseEntity.ok("Processed messages exported successfully.");
    }

    @GetMapping("/queue-size-data")
    public ResponseEntity<String> exportQueueSizeData() {
        messageReceiver.exportQueueSizeDataToCSV("C:/Users/MSI/OneDrive/Bureau/queue_size_data.csv");
        return ResponseEntity.ok("Queue size data exported successfully.");
    }
}
