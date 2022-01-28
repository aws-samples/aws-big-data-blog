// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The input of the State Indexer Worker Lambda function
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StateIndexerWorkerInput {
    /**
     * A list of S3 paths to the assigned output part files from the stateful processing EMR step
     */
    private List<String> partFiles;
}
