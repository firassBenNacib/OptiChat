package com.app.appfor.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MergedDataEntry {
    private long timestamp;
    private String message;
    private String messageNumber;
    private int queueSize;


}