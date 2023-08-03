package com.app.appfor.controller;

import com.app.appfor.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class QueueController {

    @Autowired
    private QueueService queueService;

    @Value("message Queue")
    private String queueName;

    @ResponseBody
    @GetMapping(value = "/metrics", produces = "text/plain")
    public String metrics() {
        int totalMessages = queueService.pendingJobs(queueName);
        return "# HELP messages Number of messages in the queueService\n"
                + "# TYPE messages gauge\n"
                + "messages " + totalMessages;
    }
}
