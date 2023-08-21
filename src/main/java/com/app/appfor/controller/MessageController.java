package com.app.appfor.controller;
import com.app.appfor.Component.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MessageController {

    private final MessageSender messageSender;

    @Autowired
    public MessageController(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @PostMapping("/send")
    public void sendMessage(@RequestBody String message) {
        messageSender.sendMessage(message);
    }

    @PostMapping("/send-multiple-message")
    public void sendMultipleMessage(@RequestBody String message, @RequestParam int numMessages) {

        for (int i = 0; i < numMessages; i++) {
            messageSender.sendMessage(message + " " + i);
        }
    }

    @PostMapping("/send-multiple-messages")
    public void sendMultipleMessages(@RequestBody Map<String, Integer> messages) {
        for (Map.Entry<String, Integer> entry : messages.entrySet()) {
            String message = entry.getKey();
            int numMessages = entry.getValue();

            for (int i = 0; i < numMessages; i++) {
                messageSender.sendMessage(message + " " + i);
            }
        }
    }
}

