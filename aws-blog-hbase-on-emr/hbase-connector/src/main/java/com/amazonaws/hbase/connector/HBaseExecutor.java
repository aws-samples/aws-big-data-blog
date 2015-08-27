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
package com.amazonaws.hbase.connector;

import java.util.Map;

import com.amazonaws.hbase.kinesis.KinesisConnectorExecutor;
import com.amazonaws.hbase.kinesis.KinesisMessageModel;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorRecordProcessorFactory;

/**
 * Executor to emit records to Apache HBase running on Amazon EMR. The number of records per Apache HBase put operation can be set in the buffer
 * properties.
 */
public class HBaseExecutor extends KinesisConnectorExecutor<KinesisMessageModel, Map<String,String>> {
    private static final String CONFIG_FILE = "EMRHbase.properties";

    /**
     * Creates a new HBaseExecutor.
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public HBaseExecutor(String configFile) {
        super(configFile);
    }

    @Override
    public KinesisConnectorRecordProcessorFactory<KinesisMessageModel, Map<String,String>> getKinesisConnectorRecordProcessorFactory() {
        return new KinesisConnectorRecordProcessorFactory<KinesisMessageModel, Map<String,String>>(
                new HBasePipeline(), config);
    }

    /**
     * Main method to run the HBaseExecutor.
     * 
     * @param args
     */
    public static void main(String[] args) {
        KinesisConnectorExecutor<KinesisMessageModel, Map<String,String>> hbaseExecutor = new HBaseExecutor(CONFIG_FILE);
        hbaseExecutor.run();
    }
}
