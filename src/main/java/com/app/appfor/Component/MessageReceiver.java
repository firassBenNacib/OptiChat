package com.app.appfor.Component;

import com.app.appfor.service.QueueService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MessageReceiver {

    private final AtomicBoolean stopAcceptingMessages = new AtomicBoolean(false);
    private final AtomicInteger processingCounter = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);

    private final AtomicInteger queueDifferenceMetric = new AtomicInteger(0);

    private final QueueService queueService;



    private final int batchSize = 125;
    private final long batchSleepTime = 3L * 60 * 1000;

    //private static final String DATA_FILE_PATH = "C:/Users/MSI/OneDrive/Bureau/data.csv";
    private static final String DATA_FILE_PATH = "/app/data/data.csv";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    public MessageReceiver(MeterRegistry meterRegistry, QueueService queueService) {

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
        Gauge.builder("queue_difference_metric", queueDifferenceMetric, AtomicInteger::get)
                .description("Difference between actual queue size and nearest target queue size")
                .register(meterRegistry);
    }

    @JmsListener(destination = "message Queue", concurrency = "${spring.jms.listener.concurrency:5}")
    public void receiveMessage(String message) {
        if (!stopAcceptingMessages.get()) {
            synchronized (processingCounter) {
                processingCounter.incrementAndGet();
                computeQueueDifferenceMetric();
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


        }
    }

    private boolean shouldPauseProcessing() {

        int queueSize = getPendingMessages();
        int activeProcessing = processingCounter.get();
        int targetQueueSize = 1000;

        return queueSize < targetQueueSize && activeProcessing == 0;
    }

    private void waitForResume() {

        while (processingCounter.get() == 0) {
            try {
                Thread.sleep(20000);
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
            recordDataForWeka(message);

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
    private void computeQueueDifferenceMetric() {
        int queueSize = getPendingMessages();
        int nearestTarget = (queueSize / 1000) * 1000;

        if (queueSize - nearestTarget > 0) {
            queueDifferenceMetric.set(queueSize - nearestTarget);
        } else {
            queueDifferenceMetric.set(0);
        }
    }
    private void recordDataForWeka(String messageContent) {
        try {

            FileWriter fw = new FileWriter(DATA_FILE_PATH, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            String currentTime = DATE_FORMAT.format(new Date());


            pw.println(
                    currentTime + "," +
                            getPendingMessages() + "," +
                            processingCounter.get() + "," +
                            queueDifferenceMetric.get() + "," +
                            messageContent.replace(',', ';')
            );


            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
