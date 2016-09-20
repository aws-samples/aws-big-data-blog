package com.amazonaws.bigdatablog.edba;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
import com.amazonaws.services.elasticmapreduce.model.DescribeStepRequest;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.ListClustersRequest;
import com.amazonaws.services.elasticmapreduce.model.ListClustersResult;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;
import com.amazonaws.services.elasticmapreduce.model.StepState;
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
		Connection conn = DriverManager.getConnection(url, user, password);
		List<S3EventNotificationRecord> notificationRecords = event.getRecords();
		PreparedStatement ps = conn.prepareStatement(auditValidatedFileSQL);
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
		Connection conn = DriverManager.getConnection(url, user, password);
 		Statement conditionFetchStmt = conn.createStatement();
		ResultSet rs = conditionFetchStmt.executeQuery(conditionFetchSQL);
		PreparedStatement updateJobConfigPS = conn.prepareStatement(updateJobConfigStatusSQL);
		Statement jobInputFilesMinTimestampStmt = conn.createStatement();
		Statement updateSubmittedJobsStmt=conn.createStatement();
		List<String> activeClusters = getActiveClusterId();
		String clusterId = null;
		while(rs.next()){
			ResultSet conditionQueryResult = jobInputFilesMinTimestampStmt.executeQuery(jobInputFilesMinTSAndCountSQL+rs.getString("job_input_pattern"));
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
				updateSubmittedJobsStmt.addBatch(updateSubmittedJobsJSON.replaceAll("\\?", rs.getString("job_config_id"))+rs.getString("job_input_pattern"));

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
//		Class.forName("com.mysql.jdbc.Driver").newInstance();
		List<String> stepIds = new ArrayList<String>();
		Connection conn = DriverManager.getConnection(url, user, password);
		ResultSet openStepsRS = conn.createStatement().executeQuery(retrieveOpenSteps);
		AmazonElasticMapReduceClient emr = new AmazonElasticMapReduceClient();
		DescribeStepRequest stepReq=new  DescribeStepRequest();
		PreparedStatement ps = conn.prepareStatement(updateStepStatus);
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
		emr.setRegion(Region.getRegion(Regions.US_EAST_1));
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
	
	protected List<String> getActiveClusterId() throws Exception{
		AmazonElasticMapReduceClient emrClient = new AmazonElasticMapReduceClient();
		List<String> waitingClusters = new ArrayList<String>();
		ListClustersResult clusterResult = emrClient.listClusters(new ListClustersRequest().withClusterStates(ClusterState.WAITING));
		for( ClusterSummary cluster : clusterResult.getClusters()){
				waitingClusters.add(cluster.getId());
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
	

	static{
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		}catch(Exception ce){
			ce.printStackTrace();
		}
	}
	
		final  String conditionFetchSQL = "select job_config_id, job_params,job_input_pattern,coalesce(last_run_timestamp,now() - interval 10 year) as last_run_timestamp,job_addl_criteria,coalesce(job_min_file_count,0) as job_min_file_count from edbaconfig.aggrjobconfiguration where last_exec_status is null or last_exec_status <> 'RUNNING'";
		final  String jobInputFilesMinTSAndCountSQL = "select count(file_url) as file_count,min(last_validated_timestamp) as min_lvt from edbaconfig.ingestedfilestatus where ";
	
		final  String updateJobConfigStatusSQL = "update edbaconfig.aggrjobconfiguration set last_exec_stepid=?,last_exec_status='RUNNING',last_run_timestamp=current_timestamp "
				+ " where job_config_id=? ";
		final  String updateSubmittedJobsJSON =  "update ingestedfilestatus set submitted_jobs = case when "
											+ "json_contains(submitted_jobs,json_array('?')) is null then json_array('?') when "
											+ "json_contains(submitted_jobs,json_array('?')) = 0 then json_merge(submitted_jobs,json_array('?')) "
											+ " else submitted_jobs end where " ;
		final String updateStepStatus = "update edbaconfig.aggrjobconfiguration set last_exec_status=? where job_config_id=?";
		final String retrieveOpenSteps = "select job_config_id, substr(last_exec_stepid,1,locate(':',last_exec_stepid)-1) as cluster_id,substr(last_exec_stepid,locate(':',last_exec_stepid)+1) as step_id from edbaconfig.aggrjobconfiguration  where last_exec_stepid is not null and last_exec_status = 'RUNNING'";

		final String auditValidatedFileSQL = "INSERT INTO edbaconfig.ingestedfilestatus (file_url, submitted_jobs,last_update_status, last_validated_timestamp) VALUES(?,null,?,current_timestamp) ON DUPLICATE KEY UPDATE  last_update_status = ? , submitted_jobs=null,last_validated_timestamp=current_timestamp";

		final String url = "jdbc:mysql://<<YourClusterURL>>:3306/<<YourDatabase>>";
		final String user = "<<YourUser>>";
		final String password = "<<YourPassword>>";

		
}
