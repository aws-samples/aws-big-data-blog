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
package com.amazonaws.hbase.kinesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.hbase.kinesis.utils.KinesisUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is a data source for supplying input to the Amazon Kinesis stream. It reads lines from the
 * input file specified in the constructor and emits them by calling String.getBytes() into the
 * stream defined in the KinesisConnectorConfiguration.
 */
public class StreamSource implements Runnable {
    private static Log LOG = LogFactory.getLog(StreamSource.class);
    protected AmazonKinesisClient kinesisClient;
    protected KinesisConnectorConfiguration config;
    protected final String inputFile;
    protected final boolean loopOverInputFile;
    protected ObjectMapper objectMapper;

    /**
     * Creates a new StreamSource.
     * 
     * @param config
     *        Configuration to determine which stream to put records to and get {@link AWSCredentialsProvider}
     * @param inputFile
     *        File containing record data to emit on each line
     */
    public StreamSource(KinesisConnectorConfiguration config, String inputFile) {
        this(config, inputFile, false);
    }

    /**
     * Creates a new StreamSource.
     * 
     * @param config
     *        Configuration to determine which stream to put records to and get {@link AWSCredentialsProvider}
     * @param inputFile
     *        File containing record data to emit on each line
     * @param loopOverStreamSource
     *        Loop over the stream source to continually put records
     */
    public StreamSource(KinesisConnectorConfiguration config, String inputFile, boolean loopOverStreamSource) {
        this.config = config;
        this.inputFile = inputFile;
        this.loopOverInputFile = loopOverStreamSource;
        this.objectMapper = new ObjectMapper();
        kinesisClient = new AmazonKinesisClient(config.AWS_CREDENTIALS_PROVIDER);
        kinesisClient.setRegion(RegionUtils.getRegion(config.REGION_NAME));
        if (config.KINESIS_ENDPOINT != null) {
            kinesisClient.setEndpoint(config.KINESIS_ENDPOINT);
        }
        KinesisUtils.createInputStream(config);
    }

    @Override
    public void run() {
        int iteration = 0;
        do {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(inputFile);
            if (inputStream == null) {
                throw new IllegalStateException("Could not find input file: " + inputFile);
            }
            if (loopOverInputFile) {
                LOG.info("Starting iteration " + iteration + " over input file.");
            }
            try {
                processInputStream(inputStream, iteration);
            } catch (IOException e) {
                LOG.error("Encountered exception while putting data in source stream.", e);
                break;
            }
            iteration++;
        } while (loopOverInputFile);
    }

    /**
     * Process the input file and send PutRecordRequests to Amazon Kinesis.
     * 
     * This function serves to Isolate StreamSource logic so subclasses
     * can process input files differently.
     * 
     * @param inputStream
     *        the input stream to process
     * @param iteration
     *        the iteration if looping over file
     * @throws IOException
     *         throw exception if error processing inputStream.
     */
    protected void processInputStream(InputStream inputStream, int iteration) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lines = 0;
            while ((line = br.readLine()) != null) {
                KinesisMessageModel kinesisMessageModel = objectMapper.readValue(line, KinesisMessageModel.class);

                PutRecordRequest putRecordRequest = new PutRecordRequest();
                putRecordRequest.setStreamName(config.KINESIS_INPUT_STREAM);
                putRecordRequest.setData(ByteBuffer.wrap(line.getBytes()));
                putRecordRequest.setPartitionKey(Integer.toString(kinesisMessageModel.getUserid()));
                kinesisClient.putRecord(putRecordRequest);
                lines++;
            }
            LOG.info("Added " + lines + " records to stream source.");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
