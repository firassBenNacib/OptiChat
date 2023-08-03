package com.app.appfor.Component;

import com.app.appfor.service.QueueService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MessageReceiver {

    private final AtomicBoolean stopAcceptingMessages = new AtomicBoolean(false);
    private final AtomicInteger processingCounter = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);

    private final QueueService queueService;
    private final MeterRegistry meterRegistry;

    private Gauge pendingMessagesGauge;

    @Autowired
    public MessageReceiver(MeterRegistry meterRegistry, QueueService queueService) {
        this.meterRegistry = meterRegistry;
        this.queueService = queueService;


        Gauge.builder("active_processing_messages", processingCounter, AtomicInteger::get)
                .description("Number of active processing messages")
                .register(meterRegistry);


        Gauge.builder("total_processed_messages", totalProcessedMessages, AtomicInteger::get)
                .description("Total number of messages processed by this consumer instance")
                .register(meterRegistry);

        pendingMessagesGauge = Gauge.builder("pending_messages", this, MessageReceiver::getPendingMessages)
                .description("Number of pending messages in the queue")
                .register(meterRegistry);
    }

    @JmsListener(destination = "message Queue", concurrency = "${spring.jms.listener.concurrency:5}")
    public void receiveMessage(String message) {
        if (!stopAcceptingMessages.get()) {

            synchronized (processingCounter) {
                processingCounter.incrementAndGet();
            }


            totalProcessedMessages.incrementAndGet();

            simulateProcessing(message);

            pendingMessagesGauge = Gauge.builder("pending_messages", this, MessageReceiver::getPendingMessages)
                    .description("Number of pending messages in the queue")
                    .register(meterRegistry);
        }
    }

    private void simulateProcessing(String message) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Processed message: " + message);


        synchronized (processingCounter) {
            processingCounter.decrementAndGet();
        }

        if (stopAcceptingMessages.get() && areNoMessagesProcessing()) {
            System.out.println("No more processing messages. Initiating graceful shutdown.");
            initiateGracefulShutdown();
        }
    }

    private boolean areNoMessagesProcessing() {
        return processingCounter.get() == 0;
    }

    private void initiateGracefulShutdown() {
        System.exit(0);
    }


    private int getPendingMessages() {
        return queueService.pendingJobs("message Queue");
    }
}
