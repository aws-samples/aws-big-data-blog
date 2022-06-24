package com.amazonaws.services.lambda.model;

import org.joda.time.DateTime;

public class S3Object {

    private final String bucket;
    private final String key;
    private final String[] keyParts;
    private final DateTime dateTime;

    public S3Object(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
        this.keyParts = key.split("/");
        this.dateTime = parseDateTime();
    }

    public String bucket() {
        return bucket;
    }

    public String key() {
        return key;
    }

    public String keyPart(int index){
        return keyParts[index];
    }

    public int keyLength(){
        return keyParts.length;
    }

    public boolean hasDateTimeKey() {
        return dateTime != null;
    }

    DateTime parseDateTimeFromKey() {
        return dateTime;
    }

    private DateTime parseDateTime() {
        try {
            return new DateTime(year(), month(), day(), hour(), 0);
        } catch (Exception e) {
            return null;
        }
    }

    private int year() {
        return Integer.parseInt(keyParts[keyParts.length - 5]);
    }

    private int month() {
        return Integer.parseInt(keyParts[keyParts.length - 4]);
    }

    private int day() {
        return Integer.parseInt(keyParts[keyParts.length - 3]);
    }

    private int hour() {
        return Integer.parseInt(keyParts[keyParts.length - 2]);
    }
}

