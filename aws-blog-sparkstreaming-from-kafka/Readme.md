# Real-time Stream Processing Using Apache Spark Streaming and Apache Kafka on AWS

[This post](http://blogs.aws.amazon.com/bigdata/post/Tx2CDD4Y46WIWOV/Real-time-Stream-Processing-Using-Apache-Spark-Streaming-and-Apache-Kafka-on-AWS) demonstrates how to set up Apache Kafka on [Amazon EC2](https://aws.amazon.com/ec2), use Spark Streaming on [Amazon EMR](https://aws.amazon.com/emr) to process data coming in to Apache Kafka topics, and query streaming data using Spark SQL on Amazon EMR.  

This repo provides:
- An [AWS CloudFormation](https://aws.amazon.com/cloudformation) stack to set up Apache Kafka on Amazon EC2
- Scripts/code to create the Apache Kafka topic and producer 
- Spark Streaming and Spark SQL code to run on Amazon EMR
 
For more information about how to set everything up, see the post. 

## Clone the repo
Use the following commands:

1) sudo yum install git

2) git clone https://github.com/awslabs/aws-big-data-blog.git

3) cd aws-big-data-blog/aws-blog-sparkstreaming-from-kafka


## Install the code
Use the following command:
```mvn clean install```








