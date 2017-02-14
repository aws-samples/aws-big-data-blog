package com.amazonaws.services.lambda;

import com.amazonaws.services.lambda.model.ExpirationConfig;
import com.amazonaws.services.lambda.model.PartitionConfig;
import com.amazonaws.services.lambda.model.TableService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.utils.Clock;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

public class RemoveAthenaPartitions implements RequestStreamHandler {

    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd-HH");

    private final PartitionConfig partitionConfig;
    private final ExpirationConfig expirationConfig;
    private final TableService tableService;
    private final Clock clock;

    public RemoveAthenaPartitions() {
        this(PartitionConfig.fromEnv(), ExpirationConfig.fromEnv(), new TableService(), Clock.SystemClock);
    }

    RemoveAthenaPartitions(PartitionConfig partitionConfig, ExpirationConfig expirationConfig, TableService tableService, Clock clock) {
        this.partitionConfig = partitionConfig;
        this.expirationConfig = expirationConfig;
        this.tableService = tableService;
        this.clock = clock;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Collection<String> partitionsToRemove = new HashSet<>();

        DateTime expiryThreshold = partitionConfig.partitionType().roundDownTimeUnit(clock.now())
                .minus(expirationConfig.expiresAfterMillis());

        Collection<String> existingPartitions = tableService.getExistingPartitions(partitionConfig.tableName());

        for (String existingPartition : existingPartitions) {
            DateTime partitionDateTime = partitionConfig.partitionType().roundDownTimeUnit(
                    DateTime.parse(existingPartition, DATE_TIME_PATTERN));
            if (hasExpired(partitionDateTime, expiryThreshold)) {
                partitionsToRemove.add(existingPartition);
            }
        }

        if (!partitionsToRemove.isEmpty()) {
            tableService.removePartitions(partitionConfig.tableName(), partitionsToRemove);
        }
    }

    private boolean hasExpired(DateTime partitionDateTime, DateTime expiryThreshold) {
        return partitionDateTime.isEqual(expiryThreshold) || partitionDateTime.isBefore(expiryThreshold);
    }
}
