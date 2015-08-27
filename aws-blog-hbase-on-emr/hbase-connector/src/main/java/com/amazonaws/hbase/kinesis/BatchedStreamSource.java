/*
 * Copyright 2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration;
import com.amazonaws.services.kinesis.model.PutRecordRequest;

/**
 * This class is a data source for supplying input to the Amazon Kinesis stream. It reads lines from the
 * input file specified in the constructor and batches up records before emitting them.
 */
public class BatchedStreamSource extends StreamSource {
    private static Log LOG = LogFactory.getLog(BatchedStreamSource.class);

    private static int NUM_BYTES_PER_PUT_REQUEST = 50000;
    List<KinesisMessageModel> buffer;

    public BatchedStreamSource(KinesisConnectorConfiguration config, String inputFile) {
        this(config, inputFile, false);
    }

    public BatchedStreamSource(KinesisConnectorConfiguration config, String inputFile, boolean loopOverStreamSource) {
        super(config, inputFile, loopOverStreamSource);
        buffer = new ArrayList<KinesisMessageModel>();
    }

    @Override
    protected void processInputStream(InputStream inputStream, int iteration) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lines = 0;

            while ((line = br.readLine()) != null) {
                KinesisMessageModel kinesisMessageModel = objectMapper.readValue(line, KinesisMessageModel.class);
                buffer.add(kinesisMessageModel);
                if (numBytesInBuffer() > NUM_BYTES_PER_PUT_REQUEST) {
                    /*
                     * We need to remove the last record to ensure this data blob is accepted by the Amazon Kinesis
                     * client which restricts the data blob to be less than 50 KB.
                     */
                    KinesisMessageModel lastRecord = buffer.remove(buffer.size() - 1);
                    flushBuffer();
                    /*
                     * We add it back so it will be part of the next batch.
                     */
                    buffer.add(lastRecord);
                }
                lines++;
            }
            if (!buffer.isEmpty()) {
                flushBuffer();
            }

            LOG.info("Added " + lines + " records to stream source.");
        }
    }

    private byte[] bufferToBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(buffer);
        return bos.toByteArray();
    }

    private int numBytesInBuffer() throws IOException {
        return bufferToBytes().length;
    }

    private void flushBuffer() throws IOException {
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setStreamName(config.KINESIS_INPUT_STREAM);
        putRecordRequest.setData(ByteBuffer.wrap(bufferToBytes()));
        putRecordRequest.setPartitionKey(String.valueOf(UUID.randomUUID()));
        kinesisClient.putRecord(putRecordRequest);
        buffer.clear();
    }
}
