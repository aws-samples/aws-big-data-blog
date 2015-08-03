# Serverless S3 Metadata Index
The code in this directory accompanies the AWS Big Data Blog post here: <link>

## Contents
This subtree contains the following code samples:

- **s3-index-lambda:** Simple JS implementation of a Lambda function for indexing S3 buckets
- **s3-log-generator:** Java program used to generate dummy objects and upload them to S3 in order to test the index system.
- **s3-index-example.template:** CloudFormation template for creating an S3 bucket, DynamoDB table and Lambda function.
- **createstack:** A shell script that will upload the current version of the Lambda JS code to a specified bucket and then launch the CloudFormation stack with the appropriate variables supplied.

## Deploying the sample

To deploy the sample Lambda function and its dependencies, you must create the following resources:

1. S3 bucket to be indexed
1. DynamoDB table to hold the meta-data index itself
1. IAM Role for the Lambda function to assume that grants permission for reading from the S3 bucket and writting to DynamoDB.
1. Lambda function for handling S3 object creation events