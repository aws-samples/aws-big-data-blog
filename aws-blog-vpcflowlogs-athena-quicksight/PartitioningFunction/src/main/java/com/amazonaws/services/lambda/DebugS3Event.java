package com.amazonaws.services.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

public class DebugS3Event implements RequestHandler<S3Event, Void> {

    @Override
    public Void handleRequest(S3Event s3Event, Context context) {

        System.out.println("START NOTIFICATIONS");
        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {

            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();

            System.out.printf("S3 event [Event: %s, Source: %s, Bucket: %s, Key: %s]%n", record.getEventName(), record.getEventSource(), bucket, key);

        }
        System.out.println("END NOTIFICATIONS");
        return null;
    }
}
