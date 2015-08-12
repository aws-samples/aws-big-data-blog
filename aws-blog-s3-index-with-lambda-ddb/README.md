# Serverless S3 Metadata Index
The code in this directory accompanies the AWS Big Data Blog on Building and Maintaining an Amazon S3 Metadata Index without Servers.

## Contents

This subtree contains the following code samples:

- **s3-index-lambda:** Simple JS implementation of a AWS Lambda function for indexing S3 buckets
- **s3-log-generator:** Java program used to generate dummy objects and upload them to S3 in order to test the index system.
- **query-examples:** Example scripts for querying the index
- **s3-index-example.template:** AWS CloudFormation template for creating an S3 bucket, Amazon DynamoDB table and Lambda function.

## Deploying the sample via the AWS Console

A video walkthrough of these steps is available (here)[https://s3.amazonaws.com/awsbigdatablog/S3%2BIndex%2BDeployment%2BWalkthrough.mp4].

To deploy the example index, you must create the following resources:

1. S3 bucket to be indexed
1. DynamoDB table to hold the meta-data index itself
1. IAM Role for the Lambda function to assume that grants permission for reading from the S3 bucket and writting to DynamoDB.
1. Lambda function for handling S3 object creation events


The following steps assume you have created the S3 bucket and it is named "mybucket". You should replace any instances of the string "mybucket" with the name of the bucket you have created.

### Creating the DynamoDB table

These instructions are intended to guide you through the creation of a DynamoDB table using the AWS console. You can also use the CLI to create a table with the same specification.

#### Create a DynamoDB table with the following properties

| Property |  Value          |
|----------|-----------------|
| **Primary Key Type** | Hash and Range |
| **Primary Key Hash Attribute**  | CustID |
| **Primary Key Range Attribute** | TS-ServerID |

Note: For your table name you must use the "-index" suffix with the name of your bucket. This is how the Lambda function calculates the name of table based on the S3 event.

#### Add a local secondary index with the following properties

| Property |  Value          |
|----------|-----------------|
| **Index Range Key Type** | String |
| **Index Range Key** | HasTransaction |
| **Index Name** | CustomerTransactions |
| **Projected Attributes** | Specify Attributes |
|                      |    Key             |

#### Add a global secondary index with the following properties

| Property |  Value          |
|----------|-----------------|
| **Index Hash Key Type** | String |
| **Index Hash Key**      | ServerID |
| **Index Range Key Type** | String |
| **Index Range Key**     | TS-ServerID |
| **Index Name**           | ServerIndex |
| **Projected Attributes** | Specify Attributes |
|                      |    Key             |

#### Provision throughput

You will need to provision read and write capacity for both the table itself and the global secondary index. The log generation program provided in this repository allows you to specify the rate at which objects are generated. In order to support a given object creation rate you need to provision the same amount of write capacity units for each index. For example, if you create objects at a rate of 10 per second you need a total of 20 write capacity units, 10 for the table and 10 for the ServerIndex global secondary index.

For this example you should only need to provision 1 read capacity unit for the table and the ServerIndex.

#### Finalize table creation

For the purposes of this demo you can disable streams and basic alarms.

### Creating the Lambda execution Role

In order for the Lambda function to execute with the appropriate permissions you'll need to create an IAM role for it to use.

To create a role through the console navigate to the IAM service, select Roles from the left nav, and click "Create New Role".

Enter **S3IndexFunction** for the role name.

Select **AWS Lambda** as the role type.

Attach the **AWSLambdaBasicExecutionRole** policy to the role.

After creating the role select it and add an inline policy.

Select **Custom Policy**.

Enter **s3-read-ddb-write** for the policy name.

Copy the following document into the Policy Document text area.

```JSON
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject"
            ],
            "Resource": [
                "*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:PutItem"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}
```

Apply the new policy to your role.

### Creating the Lambda Function

The final step of the deployment is to create the Lambda function that will handle our S3 bucket's creation events.

Create a new function with the following properties:

| Property |  Value          |
|----------|-----------------|
| **Name** | S3IndexFunction |
| **Runtime** | Node.js |
| **Code entry type** | Edit code inline |
| **Code** | Copy from [here](https://github.com/mikedeck/aws-big-data-blog/blob/master/aws-blog-s3-index-with-lambda-ddb/s3-index-lambda/index.js) |
| **Handler** | index.handler |
| **Role** | S3IndexFunction (created in previous step) |
| **Memory** | 128 MB |
| **Timeout** | 3 seconds |

After creating the function add an event source under the Event sources tab with the following properties.

| Property |  Value          |
|----------|-----------------|
| **Event source type** | S3 |
| **Bucket** | The bucket you wish to index |
| **Event type** | Object created |
| **Prefix** | blank |
| **Suffix** | blank |
| **Enable event source** | Enable now |

At this point any objects that are added to your S3 bucket that have a key in the correct format should be automatically added to the DynamoDB index table.

The expected key format is [4-digit hash]/[server id]/[year]-[month]-[day]-[hour]-[minute]/[customer id]-[epoch timestamp].data

Example: a5b2/i-31cc02/2015-07-05-00-25/87423-1436055953839.data

## Deploying the sample via CloudFormation

To use the provided s3-index-example.template CloudFormation template complete the following steps:

 1. Create a zip file containing the index.js file from the s3-index-lambda directory.
 2. Upload the zip file to an S3 bucket.
 3. Launch the s3-index-example.template via CloudFormation with the following parameters:
 
 	* BucketName: Name of the new bucket to create that will be indexed
 	* LambdaCodeBucket: Name of the bucket where you uploaded the zip file in step 2
 	* LambdaCodeKey: The S3 key of the zip file uploaded in step 2
 
 4. After the CloudFormation stack is created, add an event source to the Lambda function for the "Object created" event on the new S3 bucket.


## Testing the system

You can now use the (log generator)[s3-log-generator] to add objects to the S3 buckets and test the system.

After you have added some objects you can refer to the (query examples)[query-examples] to see how to use the index to run the reports and analyses discussed in the post.
