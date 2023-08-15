package com.app.appfor.Component;

import com.app.appfor.entities.ProcessedMessage;
import com.app.appfor.entities.QueueSizeDataPoint;
import com.app.appfor.service.QueueService;
import com.opencsv.CSVWriter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import com.app.appfor.weka.WekaDataLoader;
import weka.core.Instances;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MessageReceiver {

    private final AtomicBoolean stopAcceptingMessages = new AtomicBoolean(false);
    private final AtomicInteger processingCounter = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);

    private List<ProcessedMessage> processedMessages = new ArrayList<>();

    private List<QueueSizeDataPoint> queueSizeDataPoints = new ArrayList<>();
    private final QueueService queueService;

    private TreeMap<Long, MergedDataEntry> mergedDataMap = new TreeMap<>();


    private final int batchSize = 125;
    private final long batchSleepTime = 1L * 60 * 1000;

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
    }

    @JmsListener(destination = "message Queue", concurrency = "${spring.jms.listener.concurrency:5}")
    public void receiveMessage(String message) {
        int exportThreshold = 125;

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


        }
        ProcessedMessage processedMessage = new ProcessedMessage(message);
        processedMessages.add(processedMessage);

        int queueSize = getPendingMessages();


        QueueSizeDataPoint dataPoint = new QueueSizeDataPoint(queueSize);
        queueSizeDataPoints.add(dataPoint);
        long timestamp = System.currentTimeMillis();
        MergedDataEntry mergedDataEntry = new MergedDataEntry(
                timestamp,
                processedMessage.getMessage(),
                processedMessage.getMessageNumber(),
                dataPoint.getQueueSize()
        );
        mergedDataMap.put(timestamp, mergedDataEntry);

        if ((totalProcessedMessages.get() % exportThreshold == 0) || (queueSize == 0) ){
            String processedMessagesFilePath = "C:/Users/MSI/OneDrive/Bureau/processed_messages.csv";
            String queueSizeDataFilePath = "C:/Users/MSI/OneDrive/Bureau/queue_size_data.csv";
            String mergedDataFilePath = "C:/Users/MSI/OneDrive/Bureau/merged database.csv";

            exportProcessedMessagesToCSV(processedMessagesFilePath);
            exportQueueSizeDataToCSV(queueSizeDataFilePath);
            exportMergedDataToCSV(mergedDataFilePath);
        }
        try {
            Instances processedMessagesData = WekaDataLoader.loadProcessedMessagesData("C:/Users/MSI/OneDrive/Bureau/processed_messages.csv");
            Instances queueSizeData = WekaDataLoader.loadQueueSizeData("C:/Users/MSI/OneDrive/Bureau/queue_size_data.csv");

            System.out.println("Processed Messages Data:\n" + processedMessagesData);
            System.out.println("Queue Size Data:\n" + queueSizeData);
        } catch (IOException e) {
            e.printStackTrace();
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
    public void exportProcessedMessagesToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Message", "MessageNumber"};
            writer.writeNext(header);

            for (ProcessedMessage processedMessage : processedMessages) {
                String[] row = {
                        processedMessage.getTimestamp().toString(),
                        processedMessage.getMessage().replace("\"", ""),
                        processedMessage.getMessageNumber().replace("\"", "")
                };
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void exportQueueSizeDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Queue Size"};
            writer.writeNext(header);

            for (QueueSizeDataPoint dataPoint : queueSizeDataPoints) {
                String[] row = {dataPoint.getTimestamp().toString(), String.valueOf(dataPoint.getQueueSize())};
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void exportMergedDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Message", "MessageNumber", "Queue Size"};
            writer.writeNext(header);

            for (Map.Entry<Long, MergedDataEntry> entry : mergedDataMap.entrySet()) {
                MergedDataEntry mergedDataEntry = entry.getValue();
                String[] row = {
                        String.valueOf(mergedDataEntry.getTimestamp()),
                        mergedDataEntry.getMessage().replace("\"", ""),
                        mergedDataEntry.getMessageNumber().replace("\"", ""),
                        String.valueOf(mergedDataEntry.getQueueSize())
                };
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class to represent a merged data entry
    private static class MergedDataEntry {
        private long timestamp;
        private String message;
        private String messageNumber;
        private int queueSize;

        public MergedDataEntry(long timestamp, String message, String messageNumber, int queueSize) {
            this.timestamp = timestamp;
            this.message = message;
            this.messageNumber = messageNumber;
            this.queueSize = queueSize;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getMessageNumber() {
            return messageNumber;
        }

        public int getQueueSize() {
            return queueSize;
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


