package com.amazonaws.services.lambda;

import com.amazonaws.services.lambda.model.Partition;
import com.amazonaws.services.lambda.model.PartitionConfig;
import com.amazonaws.services.lambda.model.S3Object;
import com.amazonaws.services.lambda.model.TableService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class RemoveAthenaPartitionsBasedOnS3Event implements RequestHandler<S3Event, Void> {

    private final PartitionConfig partitionConfig;

    public RemoveAthenaPartitionsBasedOnS3Event() {
        this(PartitionConfig.fromEnv());
    }

    RemoveAthenaPartitionsBasedOnS3Event(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    @Override
    public Void handleRequest(S3Event s3Event, Context context) {

        Collection<Partition> partitionsToRemove = new HashSet<>();
        TableService tableService = new TableService();

        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {

            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();

            System.out.printf("S3 event [Event: %s, Bucket: %s, Key: %s]%n", record.getEventName(), bucket, key);

            S3Object s3Object = new S3Object(bucket, key);

            if (s3Object.hasDateTimeKey()) {
                partitionsToRemove.add(partitionConfig.createPartitionFor(s3Object));
            }
        }

        if (!partitionsToRemove.isEmpty()) {
            tableService.removePartitions(
                    partitionConfig.tableName(),
                    partitionsToRemove.stream().map(Partition::spec).collect(Collectors.toList()));
        }

        return null;
    }
}
