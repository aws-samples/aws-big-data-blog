// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog;

import com.amazon.aws.blog.model.StateIndexerCoordinatorInput;
import com.amazon.aws.blog.model.StateIndexerWorkerInput;
import com.amazon.aws.blog.model.StateIndexerWorkerOutput;
import com.amazonaws.services.lambda.invoke.LambdaFunction;

/**
 * Interface for the State Indexer Coordinator and Worker Lambda functions
 */
public interface StateIndexer {
    /**
     * Runs the State Indexer Coordinator Lambda function
     *
     * Responsible for listing out all part file from a stateful processing EMR step and assigning
     * one or more files to worker Lambda functions.
     *
     * @param input the input to the State Indexer Coordinator
     * @throws Exception if there is a problem running the State Indexer
     */
    void runStateIndexerCoordinator(final StateIndexerCoordinatorInput input) throws Exception;

    /**
     * Runs the State Indexer Worker Lambda function
     *
     * Responsible for indexing each assigned part file to DynamoDB.
     *
     * @param input the input to the State Indexer Worker
     * @return the output of the State Indexer Worker
     * @throws Exception if there is a problem running the State Indexer
     */
    @LambdaFunction(functionName = "RunStateIndexerWorker")
    StateIndexerWorkerOutput runStateIndexerWorker(final StateIndexerWorkerInput input) throws Exception;
}
