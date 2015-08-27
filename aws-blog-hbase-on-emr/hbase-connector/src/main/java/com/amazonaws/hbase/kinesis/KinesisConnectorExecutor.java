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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.hbase.connector.EMRHBaseKinesisConnectorConfiguration;
import com.amazonaws.hbase.kinesis.utils.EMRUtils;
import com.amazonaws.hbase.kinesis.utils.HBaseUtils;
import com.amazonaws.hbase.kinesis.utils.KinesisUtils;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.kinesis.connectors.KinesisConnectorExecutorBase;

/**
 * This class defines the execution of a Amazon Kinesis Connector.
 * 
 */
public abstract class KinesisConnectorExecutor<T, U> extends KinesisConnectorExecutorBase<T, U> {
	private static final Log LOG = LogFactory.getLog(KinesisConnectorExecutor.class);
	private static final String EMR_CLUSTER_IDENTIFIER = "emrClusterIdentifier";
	private static final String EMR_CLUSTER_NAME = "emrClusterName";
	
	//Create EMR cluster properties
	public static  final String EMR_ENDPOINT = "emrEndpoint";
	private static final String EMR_NUMBER_OF_NODES = "emrCoreNumberOfNodes";
	private static final String EMR_AMI_VERSION = "emrAmiVersion";
	private static final String EMR_MASTER_INSTANCE_TYPE = "emrMasterInstanceType";
	private static final String EMR_CORE_INSTANCE_TYPE = "emrCoreInstanceType";
	private static final String EMR_LOG_URI = "emrLogURI";
	private static final String EC2_KEYPAIR = "ec2Keypair";
	private static final int DEFAULT_EMR_NUMBER_OF_NODES = 2;

	// Create Stream Source constants
	private static final String CREATE_STREAM_SOURCE = "createStreamSource";
	private static final String LOOP_OVER_STREAM_SOURCE = "loopOverStreamSource";
	private static final boolean DEFAULT_CREATE_STREAM_SOURCE = false;
	private static final boolean DEFAULT_LOOP_OVER_STREAM_SOURCE = false;
	private static final String INPUT_STREAM_FILE = "inputStreamFile";

	// Class variables
	protected final EMRHBaseKinesisConnectorConfiguration config;
	private final Properties properties;

	/**
	 * Create a new KinesisConnectorExecutor based on the provided configuration (*.propertes) file.
	 * 
	 * @param configFile
	 *        The name of the configuration file to look for on the classpath
	 */
	public KinesisConnectorExecutor(String configFile) {
		InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);

		if (configStream == null) {
			String msg = "Could not find resource " + configFile + " in the classpath";
			throw new IllegalStateException(msg);
		}
		properties = new Properties();
		try {
			properties.load(configStream);
			configStream.close();
		} catch (IOException e) {
			String msg = "Could not load properties file " + configFile + " from classpath";
			throw new IllegalStateException(msg, e);
		}
		this.config = new EMRHBaseKinesisConnectorConfiguration(properties, getAWSCredentialsProvider());
		setupAWSResources();
		setupInputStream();

		// Initialize executor with configurations
		super.initialize(config);
	}

	/**
	 * Returns an {@link AWSCredentialsProvider} with the permissions necessary to accomplish all specified
	 * tasks. At the minimum it will require read permissions for Amazon Kinesis. Additional read permissions
	 * and write permissions may be required based on the Pipeline used.
	 * 
	 * @return
	 */
	public AWSCredentialsProvider getAWSCredentialsProvider() {
		return new DefaultAWSCredentialsProviderChain();
	}

	/**
	 * Setup necessary AWS resources for the sample. By default, the Executor will create any
	 * AWS resources specified  in the configuration file.
	 */
	private void setupAWSResources() {
		KinesisUtils.createInputStream(config);

		createEMRCluster(properties.getProperty(EMR_CLUSTER_IDENTIFIER),
						 properties.getProperty(EMR_CLUSTER_NAME),
						 properties.getProperty(EMR_AMI_VERSION),
						 properties.getProperty(EC2_KEYPAIR),
						 properties.getProperty(EMR_MASTER_INSTANCE_TYPE),
						 properties.getProperty(EMR_CORE_INSTANCE_TYPE),
						 properties.getProperty(EMR_LOG_URI),
						 parseInt(EMR_NUMBER_OF_NODES, DEFAULT_EMR_NUMBER_OF_NODES, properties));

	}

	/**
	 * Helper method to spawn the {@link StreamSource} in a separate thread.
	 */
	private void setupInputStream() {
		if (parseBoolean(CREATE_STREAM_SOURCE, DEFAULT_CREATE_STREAM_SOURCE, properties)) {
			String inputFile = properties.getProperty(INPUT_STREAM_FILE);
			StreamSource streamSource;
			if (config.BATCH_RECORDS_IN_PUT_REQUEST) {
				streamSource =
						new BatchedStreamSource(config, inputFile, parseBoolean(LOOP_OVER_STREAM_SOURCE,
								DEFAULT_LOOP_OVER_STREAM_SOURCE,
								properties));

			} else {
				streamSource =
						new StreamSource(config, inputFile, parseBoolean(LOOP_OVER_STREAM_SOURCE,
								DEFAULT_LOOP_OVER_STREAM_SOURCE,
								properties));
			}
			Thread streamSourceThread = new Thread(streamSource);
			LOG.info("Starting stream source.");
			streamSourceThread.start();
		}
	}

	
	/**
	 * Helper class to create and Amazon EMR cluster with HBase installed on that cluster
	 * 
	 * @param clusterIdentifier - cluster id if one exists
	 * @param clusterName - name you want associated with this cluster
	 * @param amiVersion - version of AMI that you wish to use for your HBase cluster
	 * @param keypair - you need a keypair to SSH into the cluster
	 * @param masterInstanceType - Amazon EC2 instance type for your master node
	 * @param coreInstanceType - Amazon Ec2 instance tyoe for your core nodes
	 * @param logUri - Specify a bucket for your EMR logs
	 * @param numberOfNodes - total number of nodes in your cluster including the master node
	 */

	private void createEMRCluster(String clusterIdentifier,
			String clusterName,
			String amiVersion,
			String keypair,
			String masterInstanceType,
			String coreInstanceType,
			String logUri,
			int numberOfNodes) {
		// Make sure the EMR cluster is available
		AmazonElasticMapReduceClient emrClient = new AmazonElasticMapReduceClient(config.AWS_CREDENTIALS_PROVIDER);
		emrClient.setEndpoint(config.EMR_ENDPOINT);
		String clusterid = clusterIdentifier;
		if (!EMRUtils.clusterExists(emrClient, clusterIdentifier)) {
			clusterid = EMRUtils.createCluster(emrClient,
						clusterIdentifier,
						amiVersion,
						keypair,
						masterInstanceType,
						coreInstanceType,
						logUri,
						numberOfNodes);
		}
		// Update the emr cluster id and public DNS properties
		config.EMR_CLUSTER_IDENTIFIER = clusterid;
		config.EMR_CLUSTER_PUBLIC_DNS = EMRUtils.getPublicDns(emrClient, clusterid);			
		//make sure table exists
		if (!HBaseUtils.tablesExists(config.HBASE_TABLE_NAME, config.EMR_CLUSTER_PUBLIC_DNS, config.HBASE_REST_PORT)){
			HBaseUtils.createTable(config.HBASE_TABLE_NAME, config.EMR_CLUSTER_PUBLIC_DNS, config.HBASE_REST_PORT);	
		}
		
	}

	/**
	 * Helper method used to parse boolean properties.
	 * 
	 * @param property
	 *        The String key for the property
	 * @param defaultValue
	 *        The default value for the boolean property
	 * @param properties
	 *        The properties file to get property from
	 * @return property from property file, or if it is not specified, the default value
	 */
	private static boolean parseBoolean(String property, boolean defaultValue, Properties properties) {
		return Boolean.parseBoolean(properties.getProperty(property, Boolean.toString(defaultValue)));
	}

	
	/**
	 * Helper method used to parse integer properties.
	 * 
	 * @param property
	 *        The String key for the property
	 * @param defaultValue
	 *        The default value for the integer property
	 * @param properties
	 *        The properties file to get property from
	 * @return property from property file, or if it is not specified, the default value
	 */
	private static int parseInt(String property, int defaultValue, Properties properties) {
		return Integer.parseInt(properties.getProperty(property, Integer.toString(defaultValue)));
	}
}
