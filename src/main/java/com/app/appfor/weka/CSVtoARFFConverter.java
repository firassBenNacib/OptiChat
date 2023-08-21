package com.app.appfor.weka;

import weka.core.converters.CSVLoader;
import weka.core.converters.ArffSaver;
import java.io.File;

public class CSVtoARFFConverter {

    public static void convert(String inputCSVFilePath, String outputARFFFilePath) {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(inputCSVFilePath));

            ArffSaver saver = new ArffSaver();
            saver.setInstances(loader.getDataSet());
            saver.setFile(new File(outputARFFFilePath));
            saver.writeBatch();

            System.out.println("ARFF file saved at: " + outputARFFFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
