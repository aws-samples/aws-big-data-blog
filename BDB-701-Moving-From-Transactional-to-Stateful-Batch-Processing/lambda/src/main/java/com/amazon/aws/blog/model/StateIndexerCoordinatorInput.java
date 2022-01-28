// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input to the State Indexer Coordinator Lambda function
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StateIndexerCoordinatorInput {
    /**
     * The S3 path to the stateful processing EMR output folder
     */
    private String statefulOutput;
}
