#Visualization of Real Time Geo-Tagged Data with Amazon Kinesis

This repoistory contains code that supports the AWS Big Data Blog article [Visualization of Real Time Geo-Tagged Data with Amazon Kinesis](http://blogs.aws.amazon.com/bigdata/).

There are three projects in this repository, **TwitterProducer** and **KinesisApplication** are Java projects that can be built with [Apache Maven](http://maven.apache.org/).  **Globe** is a node.js application.

###TwitterProducer

The code for TwitterProducer can be built with the following Maven command:

	mvn package

To run the jar file, use the following command

	java -jar target/TwitterProducer-0.0.1-SNAPSHOT.jar AwsUserData.properties 
	
AwsUserData.properties is a java properties file that contains the following information:

Property | Contents
------------ | ------------- 
aws.streamName | Name of Your Amazon Kinesis Stream
aws.regionName | Name of your region (eg us-west-1) 
twitter.consumerKey | Twitter Consumer Key  
twitter.consumerSecret | Twitter Consumer Secret Key 
twitter.token | Twitter Access Token  
twitter.secret | Twitter Access Token Secret  
twitter.hashtags | Leave empty



###KinesisApplication

The code for KinesisApplication can be built with the following Maven command:

	mvn package

To run the jar file, use the following command

	java -jar target/KinesisClient-0.0.1-SNAPSHOT.jar KinesisClient.properties 
	
KinesisClient.properties is is java properties file that contains the following information:

Property | Contents
------------ | ------------- 
appName  | Name of the application (eg DataVizAnalyzer)
kinesisEndpoint  | Name of your region (eg us-west-1) 
redisEndpoint  | The endpoint of a Redis cluster  
redisPort  | The port used by a Redis cluster 
kinesisStreamName  | Name of Your Amazon Kinesis Stream  

###Globe

**Globe** is a node.js application that can be run in Amazon Elastic Beanstalk.  Zip *the contents* of the Globe folder into an archive (ie `server.js` should be at the root level of the archive).  This archive can be used when creating the Amazon Elastic Beanstalk application with no further changes.  Supply the endpoint of the Redis cluster as PARAM1 global variable to the Amazon Elastic Beanstalk application.
