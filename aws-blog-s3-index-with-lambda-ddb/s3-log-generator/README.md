# Log Generator

This directory contains a Java project that will generate log files and upload them to Amazon S3 as if they were coming from multiple data ingestion servers.

## Running the log generator

Included in this repository is a simple program for generating dummy log files and uploading them to S3. This program simulates multiple servers ingesting data and writing the resultant log files to the specified bucket.

The log generator is written in Java. It requires Java 8 and Maven to build and run. The following instructions assume you have Maven and Java properly installed on your machine.

### Build

From the s3-log-generator directory run:

```
$ mvn compile
```

### Run

Running this example assumes you have properly configured AWS credentials set up with access to upload to your bucket. See the (documentation)[http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-config-files] on the configuring the AWS CLI for more information.

From the s3-log-generator directory run:

```
$ mvn exec:java
```

When prompted provide the following information:

```
S3 bucket: <Your Bucket Name>
Number of servers: 100
Upload rate (objects/sec): 10
```

The number of servers can be any number you choose. This simply sets how many server IDs are used when generating data.

The upload rate is also configurable, but you should make sure you have provisioned enough throughput for your DynamoDB table and global indexes in order to handle the rate you specify.
