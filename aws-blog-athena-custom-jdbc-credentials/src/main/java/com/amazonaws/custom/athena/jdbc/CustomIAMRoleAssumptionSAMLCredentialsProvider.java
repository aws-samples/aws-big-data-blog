package com.amazonaws.custom.athena.jdbc;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.simba.athena.amazonaws.auth.BasicSessionCredentials;

public class CustomIAMRoleAssumptionSAMLCredentialsProvider implements AWSCredentialsProvider{
	
	private AWSCredentials credentials;
	
	//To use in JDBC: set aws_credentials_provider_class = "com.amazonaws.custom.athena.jdbc.CustomIAMRoleAssumptionSAMLCredentialsProvider"
    // set AwsCredentialsProviderArguments = "<accessID>,<secretKey>,<sessionToken>"
	public CustomIAMRoleAssumptionSAMLCredentialsProvider(String accessId, String secretKey, String sessionToken){
	
		this.credentials = new BasicSessionCredentials(accessId,secretKey,sessionToken);
		
	}

	public AWSCredentials getCredentials() {

		return credentials;
	}
	
	public void refresh() {
	
		//Use this method if refresh token is used
	}
	
	

}
