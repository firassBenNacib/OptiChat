package com.app.appfor.Component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MessageReceiver {

    private final AtomicBoolean stopAcceptingMessages = new AtomicBoolean(false);

    @Value("${max.concurrent.messages:5}")
    private int maxConcurrentMessages;

    private int processingCounter = 0;

    @JmsListener(destination = "message Queue", concurrency = "${spring.jms.listener.concurrency:5}")
    public void receiveMessage(String message) {
        if (!stopAcceptingMessages.get()) {
            // Simulate processing time for demonstration purposes
            // You should replace this with your actual message processing logic
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
        decreaseProcessingCounter();

        // If there are no more processing messages, initiate shutdown
        if (stopAcceptingMessages.get() && areNoMessagesProcessing()) {
            System.out.println("No more processing messages. Initiating graceful shutdown.");
            initiateGracefulShutdown();
        }
    }

    private synchronized void decreaseProcessingCounter() {
        processingCounter--;
    }

    private boolean areNoMessagesProcessing() {
        return processingCounter == 0;
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
