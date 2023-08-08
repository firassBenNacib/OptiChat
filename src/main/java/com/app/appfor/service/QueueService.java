package com.app.appfor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;


import java.util.Collections;

@Component
public class QueueService  {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueService.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    private int counter = 0;

    public int pendingJobs(String queueName) {
        return jmsTemplate.browse(queueName, (s, qb) -> Collections.list(qb.getEnumeration()).size());
    }


}
