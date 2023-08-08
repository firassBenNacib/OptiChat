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

    private final int batchSize = 100;
    private final long batchSleepTime = 1 * 60 * 1000;
    private final int targetQueueSize = 1000;

    private final int queueSizeMargin = 50;

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

        Gauge.builder("pending_messages", this, MessageReceiver::getPendingMessages)
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


            if (shouldPauseProcessing()) {
                stopAcceptingMessages.set(true);
                waitForResume();
                stopAcceptingMessages.set(false);
            }

            simulateProcessing(message);


            if (processingCounter.get() == 0 && totalProcessedMessages.get() % batchSize == 0) {
                System.out.println("Completed a batch. Sleeping for " + batchSleepTime + " milliseconds.");
                try {
                    Thread.sleep(batchSleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

         Gauge.builder("pending_messages", this, MessageReceiver::getPendingMessages)
                    .description("Number of pending messages in the queue")
                    .register(meterRegistry);
        }
    }

    private boolean shouldPauseProcessing() {
        int queueSize = getPendingMessages();
        int activeProcessing = processingCounter.get();
        boolean isFalsePositive = Math.abs(queueSize - targetQueueSize) <= queueSizeMargin;

        return queueSize < targetQueueSize && activeProcessing == 0 && !isFalsePositive;
    }

    private void waitForResume() {

        while (processingCounter.get() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void simulateProcessing(String message) {
        try {
            Thread.sleep(1000);
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


        if (processingCounter.get() == 0 && totalProcessedMessages.get() % batchSize == 0) {
            System.out.println("Completed a batch. Sleeping for " + batchSleepTime + " milliseconds.");
            try {
                Thread.sleep(batchSleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
