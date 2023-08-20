package com.app.appfor.Component;

import com.app.appfor.entities.*;
import com.app.appfor.service.QueueService;
import com.opencsv.CSVWriter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;


import java.io.FileWriter;
import java.io.IOException;


import java.time.LocalDateTime;
import java.util.*;
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


    private List<ProcessedMessage> processedMessages = new ArrayList<>();

    private List<QueueSizeDataPoint> queueSizeDataPoints = new ArrayList<>();


    private List<QueueDifferenceMetricData> queueDifferenceMetricDataList = new ArrayList<>();

    private TreeMap<Long, MergedDataEntry> mergedDataMap = new TreeMap<>();
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
        int exportThreshold = 125;
        long timestamp = System.currentTimeMillis();
        LocalDateTime timestamp2 = LocalDateTime.now();

        int queueSize = 0;
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

            ProcessedMessage processedMessage = new ProcessedMessage(message);
            processedMessages.add(processedMessage);

            QueueSizeDataPoint dataPoint = new QueueSizeDataPoint(getPendingMessages());
            queueSizeDataPoints.add(dataPoint);



            QueueDifferenceMetricData queueDifferenceData = new QueueDifferenceMetricData(timestamp2,queueDifferenceMetric.get());
            queueDifferenceMetricDataList.add(queueDifferenceData);



            MergedDataEntry mergedDataEntry = new MergedDataEntry(
                    timestamp,
                    processedMessage.getMessage(),
                    processedMessage.getMessageNumber(),
                    dataPoint.getQueueSize()
            );
            mergedDataMap.put(timestamp, mergedDataEntry);

        }
        if ((totalProcessedMessages.get() % exportThreshold == 0) || (queueSize == 0)) {
            String processedMessagesFilePath = "/app/data/processed_messages.csv";
            String queueSizeDataFilePath = "/app/data/queue_size_data.csv";
            String QueueDifferenceFilePath = "/app/data/QueueDifference.csv";
            String mergedDataFilePath = "/app/data/merged_database.csv";

            exportProcessedMessagesToCSV(processedMessagesFilePath);
            exportQueueSizeDataToCSV(queueSizeDataFilePath);
            exportQueueDifferenceMetricToCSV(QueueDifferenceFilePath);
            exportMergedDataToCSV(mergedDataFilePath);
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


    public void exportQueueDifferenceMetricToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Queue Difference Metric"};
            writer.writeNext(header);

            for (QueueDifferenceMetricData data : queueDifferenceMetricDataList) {
                String[] row = {data.getTimestamp().toString(), String.valueOf(data.getMetric())};
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
}
