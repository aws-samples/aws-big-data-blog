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


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.model.BootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.Cluster;
import com.amazonaws.services.elasticmapreduce.model.ClusterSummary;
import com.amazonaws.services.elasticmapreduce.model.Command;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterResult;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowDetail;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesDetail;
import com.amazonaws.services.elasticmapreduce.model.ListBootstrapActionsRequest;
import com.amazonaws.services.elasticmapreduce.model.ListBootstrapActionsResult;
import com.amazonaws.services.elasticmapreduce.model.ListClustersResult;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.ScriptBootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;
import com.amazonaws.services.elasticmapreduce.util.StepFactory;


public class EMRUtils {
	private static Log LOG = LogFactory.getLog(EMRUtils.class);

	/**
	 * This method uses method the AWS Java to launch an Apache HBase cluster on Amazon EMR. 
	 * 
	 * @param client - AmazonElasticMapReduce client that interfaces directly with the Amazon EMR Web Service
	 * @param clusterIdentifier - identifier of an existing cluster
	 * @param amiVersion - AMI to use for launching this cluster
	 * @param keypair - A keypair for SSHing into the Amazon EMR master node
	 * @param masterInstanceType - Master node Amazon EC2 instance type 
	 * @param coreInstanceType - core nodes Amazon EC2 instance type 
	 * @param logUri - An Amazon S3 bucket for your 
	 * @param numberOfNodes - total number of nodes in this cluster including master node
	 * @return
	 */
	public static String createCluster(AmazonElasticMapReduce client,
			String clusterIdentifier,
			String amiVersion,
			String keypair,
			String masterInstanceType,
			String coreInstanceType,
			String logUri,
			int numberOfNodes) {

		if (clusterExists(client, clusterIdentifier)) {
			LOG.info("Cluster " + clusterIdentifier + " is available");
			return clusterIdentifier;
		}
		
		//Error checking
		if (amiVersion == null || amiVersion.isEmpty()) throw new RuntimeException("ERROR: Please specify an AMI Version");
		if (keypair == null || keypair.isEmpty()) throw new RuntimeException("ERROR: Please specify a valid Amazon Key Pair");
		if (masterInstanceType == null || masterInstanceType.isEmpty()) throw new RuntimeException("ERROR: Please specify a Master Instance Type");
		if (logUri == null || logUri.isEmpty()) throw new RuntimeException("ERROR: Please specify a valid Amazon S3 bucket for your logs.");
		if (numberOfNodes < 0) throw new RuntimeException("ERROR: Please specify at least 1 node");
		  		
		  RunJobFlowRequest request = new RunJobFlowRequest()
		    .withAmiVersion(amiVersion)
			.withBootstrapActions(new BootstrapActionConfig()
			             .withName("Install HBase")
			             .withScriptBootstrapAction(new ScriptBootstrapActionConfig()
			             .withPath("s3://elasticmapreduce/bootstrap-actions/setup-hbase")))
			.withName("Job Flow With HBAse Actions")	 
			.withSteps(new StepConfig() //enable debugging step
						.withName("Enable debugging")
						.withActionOnFailure("TERMINATE_CLUSTER")
						.withHadoopJarStep(new StepFactory().newEnableDebuggingStep()), 
						//Start HBase step - after installing it with a bootstrap action
						createStepConfig("Start HBase","TERMINATE_CLUSTER", "/home/hadoop/lib/hbase.jar", getHBaseArgs()), 
						//add HBase backup step
						createStepConfig("Modify backup schedule","TERMINATE_JOB_FLOW", "/home/hadoop/lib/hbase.jar", getHBaseBackupArgs()))
			.withLogUri(logUri)
			.withInstances(new JobFlowInstancesConfig()
			.withEc2KeyName(keypair)
			.withInstanceCount(numberOfNodes)
			.withKeepJobFlowAliveWhenNoSteps(true)
			.withMasterInstanceType(masterInstanceType)
			.withSlaveInstanceType(coreInstanceType));

		RunJobFlowResult result = client.runJobFlow(request);
		
		String state = null;
		while (!(state = clusterState(client, result.getJobFlowId())).equalsIgnoreCase("waiting")) {
			try {
				Thread.sleep(10 * 1000);
				LOG.info(result.getJobFlowId() + " is " + state + ". Waiting for cluster to become available.");
			} catch (InterruptedException e) {

			}
			
			if (state.equalsIgnoreCase("TERMINATED_WITH_ERRORS")){
				LOG.error("Could not create EMR Cluster");
				System.exit(-1);	
			}
		}
		LOG.info("Created cluster " + result.getJobFlowId());
		LOG.info("Cluster " + clusterIdentifier + " is available");	
		return result.getJobFlowId();
	}
	

	/**
	 * Helper method to determine if an Amazon EMR cluster exists
	 * 
	 * @param client
	 *        The {@link AmazonElasticMapReduceClient} with read permissions
	 * @param clusterIdentifier
	 *        The Amazon EMR cluster to check
	 * @return true if the Amazon EMR cluster exists, otherwise false
	 */
	public static boolean clusterExists(AmazonElasticMapReduce client, String clusterIdentifier) {
		if (clusterIdentifier != null && !clusterIdentifier.isEmpty()) {
			ListClustersResult clustersList = client.listClusters();
			ListIterator<ClusterSummary> iterator = clustersList.getClusters().listIterator();
			ClusterSummary summary;
			for (summary = iterator.next() ; iterator.hasNext();summary = iterator.next()) {
				if (summary.getId().equals(clusterIdentifier)) {
					DescribeClusterRequest describeClusterRequest = new DescribeClusterRequest().withClusterId(clusterIdentifier);	
					DescribeClusterResult result = client.describeCluster(describeClusterRequest);	
					if (result != null) {
						Cluster cluster = result.getCluster();
						//check if HBase is installed on this cluster
						if (isHBaseInstalled(client, cluster.getId())) return false;
						String state = cluster.getStatus().getState();
						LOG.info(clusterIdentifier + " is " + state + ". ");
						if (state.equalsIgnoreCase("RUNNING") ||state.equalsIgnoreCase("WAITING"))	{
							LOG.info("The cluster with id " + clusterIdentifier + " exists and is " + state);   
							return true;
						}
					}
				}		
			}					
		}
		LOG.info("The cluster with id " + clusterIdentifier + " does not exist");
		return false;  
	}
	

	/**
	 * Helper method to determine the Amazon EMR cluster state
	 * 
	 * @param client
	 *        The {@link AmazonElasticMapReduceClient} with read permissions
	 * @param clusterIdentifier
	 *        The Amazon EMR cluster to get the state of - e.g. j-2A98VJHDSU48M
	 * @return The String representation of the Amazon EMR cluster state
	 */
	public static String clusterState(AmazonElasticMapReduce client, String clusterIdentifier) {
		DescribeClusterRequest describeClusterRequest = new DescribeClusterRequest().withClusterId(clusterIdentifier);
		DescribeClusterResult result= client.describeCluster(describeClusterRequest);
		if (result != null) {
			return result.getCluster().getStatus().getState();
		}
		return null;
	}
	
	/**
	 * Helper method to determine the master node public DNS of an Amazon EMR cluster
	 * 
	 * @param client - The {@link AmazonElasticMapReduceClient} with read permissions
	 * @param clusterIdentifier - unique identifier for this cluster
	 * @return public dns url
	 */
	public static String getPublicDns(AmazonElasticMapReduce client, String clusterId) {	
		DescribeJobFlowsResult describeJobFlows=client.describeJobFlows(new DescribeJobFlowsRequest().withJobFlowIds(clusterId));
		describeJobFlows.getJobFlows();
		List<JobFlowDetail> jobFlows = describeJobFlows.getJobFlows();		
		JobFlowDetail jobflow =  jobFlows.get(0);		
		JobFlowInstancesDetail instancesDetail = jobflow.getInstances();
		LOG.info("EMR cluster public DNS is "+instancesDetail.getMasterPublicDnsName());
		return instancesDetail.getMasterPublicDnsName();
	}
	
	
	/**
	 * Helper method to determine if HBase is installed on this cluster
	 * @param client - The {@link AmazonElasticMapReduceClient} with read permissions
	 * @param clusterId - unique identifier for this cluster
	 * @return true, other throws Runtime exception
	 */
	private static boolean isHBaseInstalled(AmazonElasticMapReduce client, String clusterId) {
		ListBootstrapActionsResult bootstrapActions = client.listBootstrapActions(new ListBootstrapActionsRequest()
		                                                                              .withClusterId(clusterId));
		ListIterator<Command> iterator = bootstrapActions.getBootstrapActions().listIterator();
		while(iterator.hasNext()) {
			Command command = iterator.next(); 
			if (command.getName().equalsIgnoreCase("Install HBase")) return true;
		}
		throw new RuntimeException("ERROR: Apache HBase is not installed on this cluster!!");
	}
	
	
	/**
	 * This is a helper method for creating step configuration information
	 * @param stepName - a custom name to label this step
	 * @param actionOnFailure - options are terminate cluster, terminate job flow, contiunue
	 * @param jarPath - path to jar file - could be on S3 or  local file system
	 * @param args list of Java args to configure custom step
	 * @return
	 */
	private static StepConfig createStepConfig(String stepName, String actionOnFailure, String jarPath, List<String> args ) {
		//Start HBase step - after installing it with a bootstrap action
				StepConfig stepConfig = new StepConfig()
					.withName(stepName)
					.withActionOnFailure(actionOnFailure)
					.withHadoopJarStep(new HadoopJarStepConfig()
										.withJar(jarPath)
										.withArgs(args));
				return stepConfig;
	}
	

	/**
	 * Helper method to construct HBase arguments
	 * 
	 * @param client
	 *        The {@link AmazonEMRClient} with read permissions
	 * @param clusterIdentifier
	 *        The Amazon EMR cluster to get the state of
	 * @return The String representation of the Amazon EMR cluster state
	 */
	private static List<String> getHBaseArgs() {	    	
		List<String> hbaseArgs = new ArrayList<String>();
		hbaseArgs.add("emr.hbase.backup.Main");
		hbaseArgs.add("--start-master");
		return hbaseArgs;
	}
	
	
	/**
	 * Helper method to construct HBase arguments
	 * 
	 * @return A list of HBase arguments
	 */
	private static List<String> getHBaseBackupArgs() {	    	
		List<String> hbaseArgs = new ArrayList<String>();
		hbaseArgs.add("emr.hbase.backup.Main");
		hbaseArgs.add("--set-scheduled-backup");
		hbaseArgs.add("true");
		hbaseArgs.add("--backup-dir");
		hbaseArgs.add("s3://wdhbase/backups/kinesiscluster");
		hbaseArgs.add("--incremental-backup-time-interval");
		hbaseArgs.add("1");
		hbaseArgs.add("--incremental-backup-time-unit");
		hbaseArgs.add("hours");
		hbaseArgs.add("--start-time");
		hbaseArgs.add("now");
		hbaseArgs.add("--consistent");
		
		return hbaseArgs;
	}
}
