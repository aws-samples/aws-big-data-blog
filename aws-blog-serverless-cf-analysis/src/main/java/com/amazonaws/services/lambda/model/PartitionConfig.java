package com.amazonaws.services.lambda.model;

import com.amazonaws.services.lambda.utils.EnvironmentVariableUtils;

public class PartitionConfig {

    public static PartitionConfig fromEnv() {
        PartitionType partitionType = PartitionType.parse(
                EnvironmentVariableUtils.getOptionalEnv("PARTITION_TYPE", PartitionType.Day.name()));
        String tableName = EnvironmentVariableUtils.getMandatoryEnv("TABLE_NAME");
        String dynamoDBTableName = EnvironmentVariableUtils.getOptionalEnv("DDB_TABLE_NAME", "");

        return new PartitionConfig(partitionType, tableName, dynamoDBTableName);
    }

    private final PartitionType partitionType;
    private final String tableName;
    private final String dynamoDBTableName;

    public PartitionConfig(PartitionType partitionType, String tableName) {
        this(partitionType, tableName, "");
    }

    public PartitionConfig(PartitionType partitionType, String tableName, String dynamoDBTableName) {
        this.partitionType = partitionType;
        this.tableName = tableName;
        this.dynamoDBTableName = dynamoDBTableName;
    }

    public PartitionType partitionType() {
        return partitionType;
    }

    public String tableName() {
        return tableName;
    }

    public String dynamoDBTableName() {
        return dynamoDBTableName;
    }

    public Partition createPartitionFor(S3Object s3Object) {
        return new Partition(
                partitionType.createSpec(s3Object.parseDateTimeFromKey()),
                partitionType.createPath(s3Object));
    }

    public Partition createPartitionWithPartitionTypeAsNameFor(S3Object s3Object) {
        return new Partition(
                partitionType.createSpec(s3Object.parseDateTimeFromKey()),
                partitionType.createPath(s3Object),
                partitionType.name());
    }
}
