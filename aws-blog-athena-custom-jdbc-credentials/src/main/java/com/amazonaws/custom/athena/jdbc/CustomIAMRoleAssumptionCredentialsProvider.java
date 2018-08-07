package com.amazonaws.custom.athena.jdbc;

import com.simba.athena.amazonaws.auth.AWSStaticCredentialsProvider;
import com.simba.athena.amazonaws.auth.BasicAWSCredentials;
import com.simba.athena.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

public class CustomIAMRoleAssumptionCredentialsProvider implements com.amazonaws.auth.AWSCredentialsProvider{
	
	private final AWSCredentials credentials;
	private final String roleArn;
	private AWSCredentials assumedCredentials;
	private AWSSecurityTokenService stsClient;
	
	//To use in JDBC: set aws_credentials_provider_class = "com.amazonaws.custom.athena.jdbc.CustomIAMRoleAssumptionCredentialsProvider"
    // set AwsCredentialsProviderArguments = "<accessID>,<secretKey>,<roleArn>"
	public CustomIAMRoleAssumptionCredentialsProvider(String accessId, String secretKey, String roleArn){
		
		this.credentials = new BasicAWSCredentials(accessId,secretKey);
		this.roleArn = roleArn;
		
		stsClient = AWSSecurityTokenServiceClientBuilder.standard().withCredentials((com.amazonaws.auth.AWSCredentialsProvider) new AWSStaticCredentialsProvider(credentials)).build();
		
		refresh();
		
	}

	public AWSCredentials getCredentials() {

		return assumedCredentials;
	}

	public void refresh() {
	
		AssumeRoleResult result = stsClient.assumeRole(new AssumeRoleRequest().withRoleArn(roleArn).withRoleSessionName("athenajdbc"));
		assumedCredentials = getCredentialsFromAssumedRoleResult(result);
		
	}
	
	protected AWSCredentials getCredentialsFromAssumedRoleResult(AssumeRoleResult result){
		
		return new BasicSessionCredentials(result.getCredentials().getAccessKeyId(),
				result.getCredentials().getSecretAccessKey(),
				result.getCredentials().getSessionToken());
	}

}
