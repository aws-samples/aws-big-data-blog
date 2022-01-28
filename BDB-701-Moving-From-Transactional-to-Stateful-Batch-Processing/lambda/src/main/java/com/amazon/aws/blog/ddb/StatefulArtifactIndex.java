// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.ddb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

import static com.amazon.aws.blog.ddb.StatefulArtifactIndex.STATEFUL_ARTIFACTS_TABLE_NAME;

/**
 * The DyanmoDB model of the Stateful Artifacts index table.
 *
 * This table stores the location of each stateful artifact within a Spark part file stored in S3.
 */
@Data
@DynamoDBTable(tableName = STATEFUL_ARTIFACTS_TABLE_NAME)
public class StatefulArtifactIndex {
    public static final String STATEFUL_ARTIFACTS_TABLE_NAME = "stateful-artifacts";
    public static final String ORDER_ID = "order_id";
    public static final String FILE = "file";
    public static final String BYTE_OFFSET = "byte_offset";
    public static final String BYTE_SIZE = "byte_length";

    /**
     * The order ID (grouping key) of the stateful artifact
     */
    @DynamoDBHashKey(attributeName = ORDER_ID)
    private String orderId;

    /**
     * The part file where the stateful artifact is stored
     */
    @DynamoDBAttribute(attributeName = FILE)
    private String file;

    /**
     * The byte-offset within the part file where the stateful artifact is located
     */
    @DynamoDBAttribute(attributeName = BYTE_OFFSET)
    private String byteOffset;

    /**
     * The byte-size of the stateful artifact within the part file
     */
    @DynamoDBAttribute(attributeName = BYTE_SIZE)
    private String byteSize;
}
