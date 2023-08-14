package com.app.appfor.weka;

import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.IOException;

public class WekaDataLoader {

    public static Instances loadProcessedMessagesData(String filePath) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(filePath));
        return loader.getDataSet();
    }

    public static Instances loadQueueSizeData(String filePath) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(filePath));
        return loader.getDataSet();
    }

    public static void main(String[] args) {
        try {
            String processedMessagesFilePath = "C:/Users/MSI/OneDrive/Bureau/processed_messages.csv";
            String queueSizeDataFilePath = "C:/Users/MSI/OneDrive/Bureau/queue_size_data.csv";

            System.out.println("Loading Processed Messages Data...");
            Instances processedMessagesData = loadProcessedMessagesData(processedMessagesFilePath);
            System.out.println("Processed Messages Data:\n" + processedMessagesData);

            System.out.println("\nLoading Queue Size Data...");
            Instances queueSizeData = loadQueueSizeData(queueSizeDataFilePath);
            System.out.println("Queue Size Data:\n" + queueSizeData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
