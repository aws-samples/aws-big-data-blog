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

import java.util.Properties;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.auth.AWSCredentialsProvider;

public class EMRHBaseKinesisConnectorConfiguration extends KinesisConnectorConfiguration {
	private static final Log LOG = LogFactory.getLog(KinesisConnectorConfiguration.class);
	  // Default Amazon EMR Constants
    public static final String DEFAULT_EMR_ENDPOINT = "elasticmapreduce.us-east-1.amazonaws.com";
    public static final String DEFAULT_EMR_CLUSTER_IDENTIFIER = null;
    public static final int DEFAULT_HBASE_REST_PORT = 8080;
    public static final String DEFAULT_HBASE_TABLE_NAME = null;
    
 // Configurable program variables
    public final String EMR_ENDPOINT;
    public String EMR_CLUSTER_PUBLIC_DNS;
    public String EMR_CLUSTER_IDENTIFIER;
    public final int HBASE_REST_PORT;
    public String HBASE_TABLE_NAME;
      
    public EMRHBaseKinesisConnectorConfiguration (Properties properties, AWSCredentialsProvider credentialsProvider) {
    	super(properties, credentialsProvider);
    	
    	 //EMR Configuration
        EMR_ENDPOINT = properties.getProperty(PROP_EMR_ENDPOINT, DEFAULT_EMR_ENDPOINT);
        EMR_CLUSTER_IDENTIFIER = properties.getProperty(PROP_EMR_CLUSTER_IDENTIFIER, DEFAULT_EMR_CLUSTER_IDENTIFIER);
        HBASE_REST_PORT = getIntegerProperty(PROP_HBASE_REST_PORT, DEFAULT_HBASE_REST_PORT, properties);
        HBASE_TABLE_NAME = properties.getProperty(PROP_HBASE_TABLE_NAME, DEFAULT_HBASE_TABLE_NAME);
    }
    
    private int getIntegerProperty(String property, int defaultValue, Properties properties) {
        String propertyValue = properties.getProperty(property, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(propertyValue.trim());
        } catch (NumberFormatException e) {
            LOG.error(e);
            return defaultValue;
        }
    }
}
