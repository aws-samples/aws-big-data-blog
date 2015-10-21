package com.amazonaws.proserv;

import com.amazonaws.auth.*;

/**
 * Created by dgraeber on 7/27/2015.
 */
public class SampleAWSCredentialProvider extends AWSCredentialsProviderChain {

    public SampleAWSCredentialProvider() {
        super(new ClasspathPropertiesFileCredentialsProvider(), new InstanceProfileCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(), new EnvironmentVariableCredentialsProvider());
    }
}


