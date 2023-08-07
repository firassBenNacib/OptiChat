package com.app.appfor.controller;
import com.app.appfor.Component.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/sendfull")
    public void sendMessages(@RequestBody String message) {

        for (int i = 0; i < 30; i++) {
            messageSender.sendMessage(message + " " + i);
        }
    }
}
