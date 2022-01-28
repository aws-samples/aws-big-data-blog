// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

import static com.amazon.aws.blog.model.StatefulArtifactIndex.STATEFUL_ARTIFACTS_TABLE_NAME;

@Data
@DynamoDBTable(tableName = STATEFUL_ARTIFACTS_TABLE_NAME)
public class StatefulArtifactIndex {
    public static final String STATEFUL_ARTIFACTS_TABLE_NAME = "stateful-artifacts";
    public static final String ORDER_ID = "order_id";
    public static final String FILE = "file";
    public static final String BYTE_OFFSET = "byte_offset";
    public static final String BYTE_SIZE = "byte_length";

    @DynamoDBHashKey(attributeName = ORDER_ID)
    private String orderId;

    @DynamoDBAttribute(attributeName = FILE)
    private String file;

    @DynamoDBAttribute(attributeName = BYTE_OFFSET)
    private String byteOffset;

    @DynamoDBAttribute(attributeName = BYTE_SIZE)
    private String byteSize;
}
