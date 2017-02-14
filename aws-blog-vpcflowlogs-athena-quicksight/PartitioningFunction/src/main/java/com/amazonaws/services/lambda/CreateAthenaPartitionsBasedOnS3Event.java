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

public class CreateAthenaPartitionsBasedOnS3Event implements RequestHandler<S3Event, Void> {

    private final PartitionConfig partitionConfig;

    public CreateAthenaPartitionsBasedOnS3Event() {
        this(PartitionConfig.fromEnv());
    }

    CreateAthenaPartitionsBasedOnS3Event(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    @Override
    public Void handleRequest(S3Event s3Event, Context context) {

        Collection<Partition> requiredPartitions = new HashSet<>();
        TableService tableService = new TableService();

        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {

            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();

            System.out.printf("S3 event [Event: %s, Bucket: %s, Key: %s]%n", record.getEventName(), bucket, key);

            S3Object s3Object = new S3Object(bucket, key);

            if (s3Object.hasDateTimeKey()) {
                requiredPartitions.add(partitionConfig.createPartitionFor(s3Object));
            }
        }

        if (!requiredPartitions.isEmpty()) {
            Collection<Partition> missingPartitions = determineMissingPartitions(
                    partitionConfig.tableName(),
                    requiredPartitions,
                    tableService);
            tableService.addPartitions(partitionConfig.tableName(), missingPartitions);
        }

        return null;
    }

    // We could use DynamoDB to store a list of existing partitions – quick then to check which of the required
    // partitions already exist.
    private Collection<Partition> determineMissingPartitions(String tableName, Collection<Partition> requiredPartitions, TableService tableService) {

        Collection<String> existingPartitions = tableService.getExistingPartitions(tableName);

        return requiredPartitions.stream()
                .filter(p -> !existingPartitions.contains(p.spec()))
                .collect(Collectors.toList());
    }
}
