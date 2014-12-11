
HBase Kinesis-Enabled Sample Application
--------------------------------------------
This sample application demonstrates how to launch an Apache HBase cluster on [Amazon EMR](http://aws.amazon.com/elasticmapreduce/) using the [AWS SDK for Java](http://aws.amazon.com/documentation/sdk-for-java/) as well as how to extend the [Amazon Kinesis Connector Library](https://github.com/awslabs/amazon-kinesis-connectors) to emit data in real-time to your HBase cluster running on Amazon EMR.  

**Getting Started**

**Using the HBase Kinesis-enabled application** -- The best way to get started with this sample application is to first read Using HBase on Amazon EMR post in the [AWS Big Data Blog](http://blogs.aws.amazon.com/bigdata/).
The following instructions show you how to configure, build and execute this sample application. You can checkout, configure and execute this application using your favorite Java IDE configured with a [Maven](http://maven.apache.org/) plugin.

**Configuration**

This section shows you how to configure your application properties and setup your HBase cluster on Amazon EMR.
***Application configuration***
You will need to configure your application properties in [EMRHBase.properties](https://github.com/wmdoble/aws-big-data-blog/blob/master/aws-blog-hbase-on-emr/hbase-connector/src/main/resources/EMRHBase.properties). Key properties include:

 - `emrEndpoint`: This property allows you to select a regional endpoint to make your requests.
 - `emrClusterIdentifier`: This property is optional. You can specify a cluster identifier of an existing Amazon EMR cluster with HBase installed. If this property is left blank, this sample application with automatically launch a new Amazon EMR cluster with HBase installed.
 - `ec2KeyPair`: Specify an Amazon EC2 key pair to allow you to SSH into your cluster’s master node.
 - `emrLogUri`: Specify an Amazon S3 bucket to store Amazon EMR logs that your cluster generates. These logs are useful for debugging.

***HBase on Amazon EMR Configuration***
Once your HBase cluster on Amazon EMR is available you will need to perform the following steps as described in the blog post:

 - SSH into master node and start HBase Rest Server.
 - Configure the master node’s security group to accept traffic on port 8080.
 - Establish SSH tunnel for dynamic port forwarding on the master node.
 - Launch HBase shell.

**Running the Sample Application**

 - Edit EMRHBase.properties as described in the Application Configuration section.
   **Note:** [HBaseExecutor](https://github.com/wmdoble/aws-big-data-blog/blob/97ece6d230a9931a7d48015adf19f3873934cee3/aws-blog-hbase-on-emr/hbase-connector/src/main/java/com/amazonaws/hbase/connector/HBaseExecutor.java) uses the [DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html), which looks for credentials supplied by environment variables, system properties, or IAM role on Amazon EC2.
 - Confirm the required AWS resources exist or specify that they should be created when the sample is run.
 - Execute [HBaseExecutor](https://github.com/wmdoble/aws-big-data-blog/blob/master/aws-blog-hbase-on-emr/hbase-connector/src/main/java/com/amazonaws/hbase/connector/HBaseExecutor.java).
 - Query your HBase table from the HBase shell to verify that indeed data loading is occurring via this Amazon Kinesis-HBase connector.

----------

**Important:** These resources will incur charges on your AWS bill. It is your responsibility to delete these resources.