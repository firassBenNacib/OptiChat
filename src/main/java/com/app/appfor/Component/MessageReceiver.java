package com.app.appfor.Component;
import com.app.appfor.entities.*;
import com.app.appfor.service.QueueService;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static com.app.appfor.weka.CSVtoARFFConverter.convert;

@Component
public class MessageReceiver {

    private final AtomicBoolean stopAcceptingMessages = new AtomicBoolean(false);
    private final AtomicInteger processingCounter = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);

    private final AtomicInteger queueDifferenceMetric = new AtomicInteger(0);

    private AtomicInteger messagesSinceLastCheck = new AtomicInteger(0);

    private final QueueService queueService;

    @Value("${REPLICA_ID}")
    private String replicaId;


    private final int batchSize = 125;
    private final long batchSleepTime = 3L * 60 * 1000;

    private long lastThroughputCheckTimestamp = System.currentTimeMillis();

    private List<ProcessedMessage> processedMessages = new ArrayList<>();

    private List<QueueSizeDataPoint> queueSizeDataPoints = new ArrayList<>();


    private List<QueueDifferenceMetricData> queueDifferenceMetricDataList = new ArrayList<>();

    private List<LatencyDataPoint> latencyDataPoints = new ArrayList<>();

    private List<MessageSizeDataPoint> messageSizeDataPoints = new ArrayList<>();


    private List<ThroughputDataPoint> throughputDataPoints = new ArrayList<>();

    private List<MemoryUtilizationDataPoint> memoryUtilizationDataPoints = new ArrayList<>();


    private TreeMap<LocalDateTime, MergedDataEntry> mergedDataMap = new TreeMap<>();

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
        LocalDateTime timestamp = LocalDateTime.now();


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
            long startProcessingTime = System.currentTimeMillis();

            simulateProcessing(message);

            long endProcessingTime = System.currentTimeMillis();
            long processingLatency = endProcessingTime - startProcessingTime;
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


            QueueDifferenceMetricData queueDifferenceData = new QueueDifferenceMetricData(timestamp, queueDifferenceMetric.get());
            queueDifferenceMetricDataList.add(queueDifferenceData);

            LatencyDataPoint latencyDataPoint = new LatencyDataPoint(processingLatency);
            latencyDataPoints.add(latencyDataPoint);

            MessageSizeDataPoint messageSizeDataPoint = new MessageSizeDataPoint(message.getBytes().length);
            messageSizeDataPoints.add(messageSizeDataPoint);


            int currentMessageCount = messagesSinceLastCheck.incrementAndGet();
            long currentTime = System.currentTimeMillis();
            double currentThroughput = 0.0;


            if (currentTime - lastThroughputCheckTimestamp >= 1000) {
                currentThroughput = (double) currentMessageCount / ((currentTime - lastThroughputCheckTimestamp) / 1000.0);
                throughputDataPoints.add(new ThroughputDataPoint(timestamp, currentThroughput));

                lastThroughputCheckTimestamp = currentTime;
                messagesSinceLastCheck.set(0);
            }


            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            memoryUtilizationDataPoints.add(new MemoryUtilizationDataPoint(usedMemory));


            MergedDataEntry mergedDataEntry = new MergedDataEntry(
                    timestamp,
                    processedMessage.getMessage(),
                    processedMessage.getMessageNumber(),
                    dataPoint.getQueueSize(),
                    queueDifferenceMetric.get(),
                    processingLatency,
                    message.getBytes().length,
                    currentThroughput,
                    usedMemory
            );

            mergedDataMap.put(timestamp, mergedDataEntry);

        }
        if ((totalProcessedMessages.get() % exportThreshold == 0) || (queueSize == 0)) {
            String processedMessagesFilePath = getFilePath("processed_messages.csv");
            String queueSizeDataFilePath = getFilePath("queue_size_data.csv");
            String QueueDifferenceFilePath = getFilePath("QueueDifference.csv");
            String LatencyFilePath = getFilePath("Latency.csv");
            String MessageSizeDataFilePath = getFilePath("MessageSizeData.csv");
            String ThroughputDataPath = getFilePath("ThroughputData.csv");
            String MemoryUtilization = getFilePath("MemoryUtilization.csv");
            String mergedDataFilePath = getFilePath("merged_dataset.csv");
            String mergedARFFFilePath = getFilePath("merged_dataset.arff");
            String mergedRepArffFilePath = "/app/data/merged_rep.arff";
            String mergedRepFilePath = "/app/data/merged_rep.csv";

            exportProcessedMessagesToCSV(processedMessagesFilePath);
            exportQueueSizeDataToCSV(queueSizeDataFilePath);
            exportQueueDifferenceMetricToCSV(QueueDifferenceFilePath);
            exportLatencyDataToCSV(LatencyFilePath);
            exportMessageSizeDataToCSV(MessageSizeDataFilePath);
            exportThroughputDataToCSV(ThroughputDataPath);
            exportMemoryUtilizationToCSV(MemoryUtilization);
            exportMergedDataToCSV(mergedDataFilePath);
            exportMergedReplicaSet(mergedDataFilePath);
            convert(mergedDataFilePath, mergedARFFFilePath);
            convert(mergedRepFilePath, mergedRepArffFilePath);

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

    public void exportLatencyDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Latency (ms)"};
            writer.writeNext(header);

            for (LatencyDataPoint dataPoint : latencyDataPoints) {
                String[] row = {dataPoint.getTimestamp().toString(), String.valueOf(dataPoint.getLatencyMillis())};
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportMessageSizeDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Message Size (bytes)"};
            writer.writeNext(header);

            for (MessageSizeDataPoint dataPoint : messageSizeDataPoints) {
                String[] row = {dataPoint.getTimestamp().toString(), String.valueOf(dataPoint.getMessageSizeBytes())};
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportThroughputDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Throughput (messages/sec)"};
            writer.writeNext(header);

            for (ThroughputDataPoint dataPoint : throughputDataPoints) {
                String[] row = {String.valueOf(dataPoint.getTimestamp()), String.valueOf(dataPoint.getThroughput())};
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportMemoryUtilizationToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Timestamp", "Used Memory (bytes)"};
            writer.writeNext(header);

            for (MemoryUtilizationDataPoint dataPoint : memoryUtilizationDataPoints) {
                String[] row = {String.valueOf(dataPoint.getTimestamp()), String.valueOf(dataPoint.getUsedMemory())};
                writer.writeNext(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void exportMergedDataToCSV(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {
                    "Timestamp",
                    "Message",
                    "MessageNumber",
                    "Queue Size",
                    "Queue Difference Metric",
                    "Latency (ms)",
                    "Message Size (bytes)",
                    "Throughput (messages/sec)",
                    "Used Memory (bytes)"

            };
            writer.writeNext(header);

            for (Map.Entry<LocalDateTime, MergedDataEntry> entry : mergedDataMap.entrySet()) {
                MergedDataEntry mergedDataEntry = entry.getValue();
                String[] row = {
                        String.valueOf(mergedDataEntry.getTimestamp()),
                        mergedDataEntry.getMessage().replace("\"", ""),
                        mergedDataEntry.getMessageNumber().replace("\"", ""),
                        String.valueOf(mergedDataEntry.getQueueSize()),
                        String.valueOf(mergedDataEntry.getQueueDifferenceMetric()),
                        String.valueOf(mergedDataEntry.getLatencyMillis()),
                        String.valueOf(mergedDataEntry.getMessageSizeBytes()),
                        String.valueOf(mergedDataEntry.getThroughput()),
                        String.valueOf(mergedDataEntry.getUsedMemory())

                };
                writer.writeNext(row);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void exportMergedReplicaSet(String mergedcsvFilePath) {

        String mergedRepFilePath = "/app/data/merged_rep.csv";

        String[] header = {
                "Timestamp",
                "Message",
                "MessageNumber",
                "Queue Size",
                "Queue Difference Metric",
                "Latency (ms)",
                "Message Size (bytes)",
                "Throughput (messages/sec)",
                "Used Memory (bytes)"
        };

        try {

            List<String[]> existingData = new ArrayList<>();
            try (Reader reader = new FileReader(mergedRepFilePath);
                 CSVReader csvReader = new CSVReader(reader)) {
                existingData.addAll(csvReader.readAll());
            } catch (FileNotFoundException e) {

                existingData.add(header);
            }


            try (Reader reader = new FileReader(mergedcsvFilePath);
                 CSVReader csvReader = new CSVReader(reader)) {
                List<String[]> newData = csvReader.readAll();


                for (int i = 1; i < newData.size(); i++) {
                    String[] row = newData.get(i);


                    boolean isDuplicate = false;
                    for (String[] existingRow : existingData) {
                        if (Arrays.equals(row, existingRow)) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        existingData.add(row);
                    }
                }
            } catch (CsvException e) {
                throw new RuntimeException(e);
            }


            try (Writer writer = new FileWriter(mergedRepFilePath);
                 CSVWriter csvWriter = new CSVWriter(writer)) {
                csvWriter.writeAll(existingData);
            }




        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    private String getBaseDirectory() {
        String baseDir = "/app/data/" + replicaId;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
            return baseDir;

    }

    private String getFilePath(String baseFileName) {
        return getBaseDirectory() + "/" + baseFileName;
    }


}
