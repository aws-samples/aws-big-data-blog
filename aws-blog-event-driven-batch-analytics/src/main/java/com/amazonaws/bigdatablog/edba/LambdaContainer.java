package com.amazonaws.bigdatablog.edba;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.services.elasticmapreduce.model.AddJobFlowStepsResult;
import com.amazonaws.services.elasticmapreduce.model.Application;
import com.amazonaws.services.elasticmapreduce.model.ClusterState;
import com.amazonaws.services.elasticmapreduce.model.ClusterSummary;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeStepRequest;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.ListClustersRequest;
import com.amazonaws.services.elasticmapreduce.model.ListClustersResult;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;
import com.amazonaws.services.elasticmapreduce.model.StepState;
import com.amazonaws.services.elasticmapreduce.model.Tag;
import com.amazonaws.services.elasticmapreduce.util.StepFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;

public class LambdaContainer {

	//Validation/Conversion Layer function
	
	public void validateAndNormalizeInputData(S3Event event,Context ctx) throws Exception{
		AmazonS3 s3Client;
		InputStream inputFileStream=null;
		InputStream readableDataStream=null;
				List<S3EventNotificationRecord> notificationRecords = event.getRecords();
				s3Client = new AmazonS3Client();
				String eventFileName,siteName,dbfName;
				CSVParser fileParser = null;
				for(S3EventNotificationRecord record : notificationRecords){
					 eventFileName = record.getS3().getObject().getKey();
					 S3Object s3Object = s3Client.getObject(new GetObjectRequest(record.getS3().getBucket().getName(), record.getS3().getObject().getKey()));
					 inputFileStream = s3Object.getObjectContent();
					 fileParser = new CSVParser(new InputStreamReader(inputFileStream),CSVFormat.TDF.withCommentMarker('-'));
					 List<CSVRecord> records = fileParser.getRecords();
					 StringWriter writer = new StringWriter();
					 CSVPrinter printer =null;
					 if(records.get(0).toString().matches(".*[^0-9].*")){
						 records.remove(0);
					 }
					 printer = new CSVPrinter(writer,CSVFormat.DEFAULT.withRecordSeparator(System.getProperty("line.separator")));
					 printer.printRecords(records);
					 printer.flush();
		        	 readableDataStream = new ByteArrayInputStream(writer.toString().getBytes("utf-8"));
		        	 s3Client.putObject(record.getS3().getBucket().getName(),"validated/"+eventFileName+".csv",readableDataStream,new ObjectMetadata());
		        	 printer.close();
		        	 readableDataStream.close();
		        }
	
		}
	

	// Tracking Input Layer lambda function
	public void auditValidatedFile(S3Event event,Context ctx) throws Exception{
		Connection conn  = new com.mysql.jdbc.Driver().connect(props.getProperty("url"), props);
		List<S3EventNotificationRecord> notificationRecords = event.getRecords();
		PreparedStatement ps = conn.prepareStatement(props.getProperty("sql.auditValidatedFile"));
		for(S3EventNotificationRecord record : notificationRecords){
			String fileURL = record.getS3().getBucket().getName()+"/"+record.getS3().getObject().getKey();
		       ps.setString(1, fileURL);
		       ps.setString(2, "VALIDATED");
		       ps.setString(3,"VALIDATED");
		       ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		conn.close();		
	}
	

	// EMR Job  Criteria Check and Submission lambda function
	
	public void checkConditionStatusAndFireEMRStep() throws Exception{
		Connection conn  = new com.mysql.jdbc.Driver().connect(props.getProperty("url"), props);
 		Statement conditionFetchStmt = conn.createStatement();
		ResultSet rs = conditionFetchStmt.executeQuery(props.getProperty("sql.conditionFetch"));
		PreparedStatement updateJobConfigPS = conn.prepareStatement(props.getProperty("sql.updateJobConfigStatus"));
		Statement jobInputFilesMinTimestampStmt = conn.createStatement();
		Statement updateSubmittedJobsStmt=conn.createStatement();
		List<String> activeClusters = getActiveTaggedClusters();
		String clusterId = null;
		while(rs.next()){
			System.out.println("job_input_pattern ::"+rs.getString("job_input_pattern"));
			System.out.println("sql.jobInputFilesMinTSAndCount :: "+props.getProperty("sql.jobInputFilesMinTSAndCount"));
			ResultSet conditionQueryResult = jobInputFilesMinTimestampStmt.executeQuery(props.getProperty("sql.jobInputFilesMinTSAndCount")+" "+rs.getString("job_input_pattern"));
			conditionQueryResult.next();
			if(conditionQueryResult.getTimestamp("min_lvt").after(rs.getTimestamp("last_run_timestamp")) 
					&&
					conditionQueryResult.getInt("file_count") >= rs.getInt("job_min_file_count")
					&&
					isAdditionalCriteriaPassed(rs.getString("job_addl_criteria"),conn)){
				clusterId = activeClusters.get(new Random().nextInt(activeClusters.size()-0));
				String jobId = fireEMRJob(rs.getString("job_params"),clusterId);
				updateJobConfigPS.setString(1,clusterId+":"+jobId);
				updateJobConfigPS.setString(2, rs.getString("job_config_id"));
				updateJobConfigPS.addBatch();
				updateSubmittedJobsStmt.addBatch(props.getProperty("sql.updateSubmittedJobsJSON").replaceAll("\\?", rs.getString("job_config_id"))+" "+rs.getString("job_input_pattern"));

			}
		}
		updateJobConfigPS.executeBatch();
		updateSubmittedJobsStmt.executeBatch();
		updateSubmittedJobsStmt.close();
		updateJobConfigPS.close();
		conditionFetchStmt.close();
		conn.close();
	}
	
	// EMR Job Monitor lambda function
	
	public void monitorEMRStep() throws Exception {
		List<String> stepIds = new ArrayList<String>();
		Connection conn  = new com.mysql.jdbc.Driver().connect(props.getProperty("url"), props);
		ResultSet openStepsRS = conn.createStatement().executeQuery(props.getProperty("sql.retrieveOpenSteps"));
		AmazonElasticMapReduceClient emr = new AmazonElasticMapReduceClient();
		DescribeStepRequest stepReq=new  DescribeStepRequest();
		PreparedStatement ps = conn.prepareStatement(props.getProperty("sql.updateStepStatus"));
		while(openStepsRS.next()){
			
			stepReq.setClusterId(openStepsRS.getString("cluster_id"));
			stepReq.setStepId(openStepsRS.getString("step_id"));
			String stepState = emr.describeStep(stepReq).getStep().getStatus().getState();
			
				if(stepState.equals(StepState.COMPLETED.toString())){
					ps.setString(1,StepState.COMPLETED.toString());
				}else if (stepState.equals(StepState.FAILED.toString())){
					ps.setString(1,StepState.FAILED.toString());					
				}
				ps.setString(2,openStepsRS.getString("job_config_id"));
				ps.addBatch();
		}
		
		ps.executeBatch();
		ps.close();
		conn.close();
	}
	
	// adds EMR step
	protected String fireEMRJob(String paramsStr,String clusterId){
		StepFactory stepFactory = new StepFactory();
		AmazonElasticMapReduceClient emr = new AmazonElasticMapReduceClient();
		emr.setRegion(Region.getRegion(Regions.fromName(System.getenv().get("AWS_REGION"))));
		Application sparkConfig = new Application()
				.withName("Spark");
		
		String[] params = paramsStr.split(",");
		StepConfig enabledebugging = new StepConfig()
				.withName("Enable debugging")
				.withActionOnFailure("TERMINATE_JOB_FLOW")
				.withHadoopJarStep(stepFactory.newEnableDebuggingStep());
		
		HadoopJarStepConfig sparkStepConf = new HadoopJarStepConfig()
				.withJar("command-runner.jar")
				.withArgs(params);	
		
		final StepConfig sparkStep = new StepConfig()
				.withName("Spark Step")
				.withActionOnFailure("CONTINUE")
				.withHadoopJarStep(sparkStepConf);

		
		AddJobFlowStepsRequest request = new AddJobFlowStepsRequest(clusterId)
				.withSteps(new ArrayList<StepConfig>(){{add(sparkStep);}});
				

		AddJobFlowStepsResult result = emr.addJobFlowSteps(request);
		return result.getStepIds().get(0);
	}
	
	protected List<String> getActiveTaggedClusters() throws Exception{
		AmazonElasticMapReduceClient emrClient = new AmazonElasticMapReduceClient();
		List<String> waitingClusters = new ArrayList<String>();
		ListClustersResult clusterResult = emrClient.listClusters(new ListClustersRequest().withClusterStates(ClusterState.WAITING));
		
		DescribeClusterRequest specifcTagDescribe = new DescribeClusterRequest();
		specifcTagDescribe.putCustomQueryParameter("Cluster.Tags",null);
		 for( ClusterSummary cluster : clusterResult.getClusters()){
			 	System.out.println("list cluster id "+cluster.getId());
			 	List<Tag> tagList = emrClient.describeCluster(specifcTagDescribe.withClusterId(cluster.getId())).getCluster().getTags();
			 	for(Tag tag:tagList){
			 		if(tag.getKey().equals(props.getProperty("edba.cluster.tag.key"))){
			 			waitingClusters.add(cluster.getId());
			 		}
			 	}
			 	
		}
		return waitingClusters;
		
	}
	/**
	 * Checks whether additional criteria returned a non empty resultset.
	 */
	protected boolean isAdditionalCriteriaPassed(String sql, Connection conn) throws Exception{
		if(StringUtils.isNullOrEmpty(sql)){
			return true;
		}
		ResultSet rs = conn.createStatement().executeQuery(sql);
		if(!rs.next()){
			return false; // Empty Resultset
		}
		return true;
	}
	

	static Properties props=null;
	static{
		try{
			props = new Properties();
			props.load(LambdaContainer.class.getResourceAsStream("/edba_lambda_config.properties"));
			
		}catch(Exception ce){
			ce.printStackTrace();
			
		}
	}
	
		
}
