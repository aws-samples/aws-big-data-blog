## Synopsis

This blog post will demonstrate how to set up Kafka on Amazon EC2, use Spark Streaming on Amazon EMR to process data coming in to Apache Kafka topics, and query streaming data using Spark SQL on Amazon EMR.

- it provides Cloudformation's to set up Kafka on Amazon EC2
- scripts/code to create Kafka topic and Kafka producer 
- Spark streaming and Spark SQL code to run on Amazon EMR
    -- the post goes into detail all the steps necessary to set up


## Install

1) sudo yum install git

2) git clone https://github.com/awslabs/aws-big-data-blog.git

3) cd aws-big-data-blog/aws-blog-sparkstreaming-from-kafka


## Compile
```mvn clean install```








