// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The output of the State Indexer Worker Lambda function
 */
@Data
@NoArgsConstructor
public class StateIndexerWorkerOutput {
    /**
     * The number of artifacts indexed to DynamoDB (can be used for checksum validation)
     */
    private Integer numArtifactsIndexed;
}
