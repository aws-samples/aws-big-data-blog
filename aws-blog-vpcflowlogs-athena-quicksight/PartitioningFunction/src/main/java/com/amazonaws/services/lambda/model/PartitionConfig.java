package com.amazonaws.services.lambda.model;

import com.amazonaws.services.lambda.utils.EnvironmentVariableUtils;

public class PartitionConfig {

    public static PartitionConfig fromEnv() {
        PartitionType partitionType = PartitionType.parse(
                EnvironmentVariableUtils.getOptionalEnv("PARTITION_TYPE", PartitionType.Day.name()));
        String tableName = EnvironmentVariableUtils.getMandatoryEnv("TABLE_NAME");

        return new PartitionConfig(partitionType, tableName);
    }

    private final PartitionType partitionType;
    private final String tableName;

    public PartitionConfig(PartitionType partitionType, String tableName) {
        this.partitionType = partitionType;
        this.tableName = tableName;
    }

    public PartitionType partitionType() {
        return partitionType;
    }

    public String tableName() {
        return tableName;
    }

    public Partition createPartitionFor(S3Object s3Object) {
        return new Partition(
                partitionType.createSpec(s3Object.parseDateTimeFromKey()),
                partitionType.createPath(s3Object));
    }
}
