/*
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.hbase.kinesis.utils;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DeleteStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;

/**
 * Utilities to create and delete Amazon Kinesis streams.
 */
public class KinesisUtils {

    private static Log LOG = LogFactory.getLog(KinesisUtils.class);

    /**
     * Creates the Amazon Kinesis stream specified by config.KINESIS_INPUT_STREAM
     * 
     * @param config
     *        The configuration with the specified input stream name and {@link AWSCredentialsProvider}
     * @param shardCount
     *        The shard count to create the stream with
     */
    public static void createInputStream(KinesisConnectorConfiguration config) {
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(config.AWS_CREDENTIALS_PROVIDER);
        kinesisClient.setRegion(RegionUtils.getRegion(config.REGION_NAME));
        if (config.KINESIS_ENDPOINT != null) {
            kinesisClient.setEndpoint(config.KINESIS_ENDPOINT);
        }
        createAndWaitForStreamToBecomeAvailable(kinesisClient,
                config.KINESIS_INPUT_STREAM,
                config.KINESIS_INPUT_STREAM_SHARD_COUNT);
    }

    /**
     * Creates the Amazon Kinesis stream specified by config.KINESIS_OUTPUT_STREAM.
     * 
     * @param config
     *        The configuration with the specified output stream name and {@link AWSCredentialsProvider}
     * @param shardCount
     *        The shard count to create the stream with
     */
    public static void createOutputStream(KinesisConnectorConfiguration config) {
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(config.AWS_CREDENTIALS_PROVIDER);
        kinesisClient.setRegion(RegionUtils.getRegion(config.REGION_NAME));
        if (config.KINESIS_ENDPOINT != null) {
            kinesisClient.setEndpoint(config.KINESIS_ENDPOINT);
        }
        createAndWaitForStreamToBecomeAvailable(kinesisClient,
                config.KINESIS_OUTPUT_STREAM,
                config.KINESIS_OUTPUT_STREAM_SHARD_COUNT);
    }

    /**
     * Creates an Amazon Kinesis stream if it does not exist and waits for it to become available
     * 
     * @param kinesisClient
     *        The {@link AmazonKinesisClient} with Amazon Kinesis read and write privileges
     * @param streamName
     *        The Amazon Kinesis stream name to create
     * @param shardCount
     *        The shard count to create the stream with
     * @throws IllegalStateException
     *         Invalid Amazon Kinesis stream state
     * @throws IllegalStateException
     *         Stream does not go active before the timeout
     */
    public static void createAndWaitForStreamToBecomeAvailable(AmazonKinesisClient kinesisClient,
            String streamName,
            int shardCount) {
        if (streamExists(kinesisClient, streamName)) {
            String state = streamState(kinesisClient, streamName);
            switch (state) {
                case "DELETING":
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + 1000 * 120;
                    while (System.currentTimeMillis() < endTime && streamExists(kinesisClient, streamName)) {
                        try {
                            LOG.info("...Deleting Stream " + streamName + "...");
                            Thread.sleep(1000 * 10);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (streamExists(kinesisClient, streamName)) {
                        LOG.error("KinesisUtils timed out waiting for stream " + streamName + " to delete");
                        throw new IllegalStateException("KinesisUtils timed out waiting for stream " + streamName
                                + " to delete");
                    }
                case "ACTIVE":
                    LOG.info("Stream " + streamName + " is ACTIVE");
                    return;
                case "CREATING":
                    break;
                case "UPDATING":
                    LOG.info("Stream " + streamName + " is UPDATING");
                    return;
                default:
                    throw new IllegalStateException("Illegal stream state: " + state);
            }
        } else {
            CreateStreamRequest createStreamRequest = new CreateStreamRequest();
            createStreamRequest.setStreamName(streamName);
            createStreamRequest.setShardCount(shardCount);
            kinesisClient.createStream(createStreamRequest);
            LOG.info("Stream " + streamName + " created");
        }
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(1000 * 10);
            } catch (Exception e) {
            }
            try {
                String streamStatus = streamState(kinesisClient, streamName);
                if (streamStatus.equals("ACTIVE")) {
                    LOG.info("Stream " + streamName + " is ACTIVE");
                    return;
                }
            } catch (ResourceNotFoundException e) {
                throw new IllegalStateException("Stream " + streamName + " never went active");
            }
        }
    }

    /**
     * Helper method to determine if an Amazon Kinesis stream exists.
     * 
     * @param kinesisClient
     *        The {@link AmazonKinesisClient} with Amazon Kinesis read privileges
     * @param streamName
     *        The Amazon Kinesis stream to check for
     * @return true if the Amazon Kinesis stream exists, otherwise return false
     */
    private static boolean streamExists(AmazonKinesisClient kinesisClient, String streamName) {
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
        describeStreamRequest.setStreamName(streamName);
        try {
            kinesisClient.describeStream(describeStreamRequest);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Return the state of a Amazon Kinesis stream.
     * 
     * @param kinesisClient
     *        The {@link AmazonKinesisClient} with Amazon Kinesis read privileges
     * @param streamName
     *        The Amazon Kinesis stream to get the state of
     * @return String representation of the Stream state
     */
    private static String streamState(AmazonKinesisClient kinesisClient, String streamName) {
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
        describeStreamRequest.setStreamName(streamName);
        try {
            return kinesisClient.describeStream(describeStreamRequest).getStreamDescription().getStreamStatus();
        } catch (AmazonServiceException e) {
            return null;
        }
    }

    /**
     * Gets a list of all Amazon Kinesis streams
     * 
     * @param kinesisClient
     *        The {@link AmazonKinesisClient} with Amazon Kinesis read privileges
     * @return list of Amazon Kinesis streams
     */
    public static List<String> listAllStreams(AmazonKinesisClient kinesisClient) {

        ListStreamsRequest listStreamsRequest = new ListStreamsRequest();
        listStreamsRequest.setLimit(10);
        ListStreamsResult listStreamsResult = kinesisClient.listStreams(listStreamsRequest);
        List<String> streamNames = listStreamsResult.getStreamNames();
        while (listStreamsResult.isHasMoreStreams()) {
            if (streamNames.size() > 0) {
                listStreamsRequest.setExclusiveStartStreamName(streamNames.get(streamNames.size() - 1));
            }

            listStreamsResult = kinesisClient.listStreams(listStreamsRequest);
            streamNames.addAll(listStreamsResult.getStreamNames());
        }
        return streamNames;
    }

    /**
     * Deletes the input stream specified by config.KINESIS_INPUT_STREAM
     * 
     * @param config
     *        The configuration containing the stream name and {@link AWSCredentialsProvider}
     */
    public static void deleteInputStream(KinesisConnectorConfiguration config) {
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(config.AWS_CREDENTIALS_PROVIDER);
        kinesisClient.setRegion(RegionUtils.getRegion(config.REGION_NAME));
        if (config.KINESIS_ENDPOINT != null) {
            kinesisClient.setEndpoint(config.KINESIS_ENDPOINT);
        }
        deleteStream(kinesisClient, config.KINESIS_INPUT_STREAM);
    }

    /**
     * Deletes the output stream specified by config.KINESIS_OUTPUT_STREAM
     * 
     * @param config
     *        The configuration containing the stream name and {@link AWSCredentialsProvider}
     */
    public static void deleteOutputStream(KinesisConnectorConfiguration config) {
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(config.AWS_CREDENTIALS_PROVIDER);
        kinesisClient.setRegion(RegionUtils.getRegion(config.REGION_NAME));
        if (config.KINESIS_ENDPOINT != null) {
            kinesisClient.setEndpoint(config.KINESIS_ENDPOINT);
        }
        deleteStream(kinesisClient, config.KINESIS_OUTPUT_STREAM);
    }

    /**
     * Deletes an Amazon Kinesis stream if it exists.
     * 
     * @param kinesisClient
     *        The {@link AmazonKinesisClient} with Amazon Kinesis read and write privileges
     * @param streamName
     *        The Amazon Kinesis stream to delete
     */
    public static void deleteStream(AmazonKinesisClient kinesisClient, String streamName) {
        if (streamExists(kinesisClient, streamName)) {
            DeleteStreamRequest deleteStreamRequest = new DeleteStreamRequest();
            deleteStreamRequest.setStreamName(streamName);
            kinesisClient.deleteStream(deleteStreamRequest);
            LOG.info("Deleting stream " + streamName);
        } else {
            LOG.warn("Stream " + streamName + " does not exist");
        }
    }

}
