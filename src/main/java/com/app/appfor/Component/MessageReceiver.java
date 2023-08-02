package com.app.appfor.Component;

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
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);

    @Autowired
    public MessageReceiver(MeterRegistry meterRegistry) {
        // Register the message_queue_size metric as a Gauge
        Gauge.builder("message_queue_size", queueSize, AtomicInteger::get)
                .description("Size of the message queue")
                .register(meterRegistry);

        // Register the active_processing_messages metric as a Gauge
        Gauge.builder("active_processing_messages", processingCounter, AtomicInteger::get)
                .description("Number of active processing messages")
                .register(meterRegistry);

        // Register the total_processed_messages metric as a Gauge
        Gauge.builder("total_processed_messages", totalProcessedMessages, AtomicInteger::get)
                .description("Total number of messages processed by this consumer instance")
                .register(meterRegistry);
    }

    @JmsListener(destination = "message Queue", concurrency = "${spring.jms.listener.concurrency:5}")
    public void receiveMessage(String message) {
        if (!stopAcceptingMessages.get()) {
            // Increment the queue size when a new message is received
            synchronized (queueSize) {
                queueSize.incrementAndGet();
            }

            // Increment the active_processing_messages counter before processing a message
            synchronized (processingCounter) {
                processingCounter.incrementAndGet();
            }

            // Increment the total_processed_messages counter when a new message is received
            totalProcessedMessages.incrementAndGet();

            simulateProcessing(message);
        }
    }


    private void simulateProcessing(String message) {
        // Simulate processing time (e.g., sleep for 3 seconds)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Processed message: " + message);

        // Decrease the processing counter after message processing
        synchronized (processingCounter) {
            processingCounter.decrementAndGet();
        }

        // Decrease the queue size after message processing
        synchronized (queueSize) {
            queueSize.decrementAndGet();
        }

        // Calculate the total processed messages (current queue size + total processed)
        int totalProcessed = queueSize.get() + totalProcessedMessages.get();
        totalProcessedMessages.set(totalProcessed);

        if (stopAcceptingMessages.get() && areNoMessagesProcessing()) {
            System.out.println("No more processing messages. Initiating graceful shutdown.");
            initiateGracefulShutdown();
        }
    }


    private void decreaseProcessingCounter() {
        processingCounter.decrementAndGet();
    }

    private boolean areNoMessagesProcessing() {
        return processingCounter.get() == 0;
    }

    private void initiateGracefulShutdown() {
        // You can add cleanup and finalization tasks here if needed
        // ...

        // Perform graceful shutdown
        System.exit(0);
    }

    public void stopAcceptingMessages() {
        stopAcceptingMessages.set(true);
    }
}
