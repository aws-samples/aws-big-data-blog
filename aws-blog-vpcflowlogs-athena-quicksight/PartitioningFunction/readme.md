Description
-----------

Creates partitions in Athena on behalf of files added to S3 that use a `/year/month/day/hour/` key prefix.

Build
-----

As a one-off operation, you'll need to install the Athena JDBC driver into a lib folder, and then add it to your local Maven repository so that it can be incorporated into the final jar:

```
mkdir lib
aws s3 cp s3://athena-downloads/drivers/AthenaJDBC41-1.0.0.jar lib/
mvn install:install-file -Dfile=lib/AthenaJDBC41-1.0.0.jar -DgroupId=com.amazonaws -DartifactId=athena.jdbc41 -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true
```

And then, to build:

```
mvn clean compile verify
```

Create an IAM Role
------------------

Before you create a Lambda function, you will need to create an IAM role that allows Lambda to execute queries in Athena. Create a role named `lambda_athena_exec_role` and attach the following managed policies to the role: AmazonS3FullAccess, AmazonAthenaFullAccess.

Add this inline access policy:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        }
    ]
}
```

And attach the following trust relationship to enable Lambda to assume the role:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

Create a Lambda Function to Add Partitions to Athena
----------------------------------------------------

Create a Lambda function that can be associated with S3 new object event notifications. When creating the function, you'll need to set several environment variables:

 - `PARTITION_TYPE` Supply one of the following values: `Month`, `Day` or `Hour`. This environment variable is optional: if you omit it, the function will default to `Day`.
 - `TABLE_NAME` Use the format ``<database>.<table_name>`. For example, `sampledb.vpc_flow_logs`.
 - `S3_STAGING_DIR` An Amazon S3 location to which your query output will be written. (Although the Lambda function is only executing DDL statements, Athena still writes an output file to S3.)
 - `ATHENA_REGION` The region in which Athena is located (e.g. `us-east-1`).

Specify the handler and an existing role:

 - *Handler:* `com.amazonaws.services.lambda.CreateAthenaPartitionsBasedOnS3Event::handleRequest`
 - *Existing role:* `lambda_athena_exec_role`

Set the timeout to one minute.

Create a Lambda Function to Remove Partitions from Athena
---------------------------------------------------------

Create a Lambda function that can be scheduled using CloudWatch Events. When creating the function, you'll need to set several environment variables:

 - `PARTITION_TYPE` Supply one of the following values: `Month`, `Day` or `Hour`. This environment variable is optional: if you omit it, the function will default to `Day`.
 - `TABLE_NAME` Use the format ``<database>.<table_name>`. For example, `sampledb.vpc_flow_logs`.
 - `EXPIRES_AFTER` Specify the expiration period in hours or days, e.g. `12h` or `30d`.
 - `S3_STAGING_DIR` An Amazon S3 location to which your query output will be written. (Although the Lambda function is only executing DDL statements, Athena still writes an output file to S3.)
 - `ATHENA_REGION` The region in which Athena is located (e.g. `us-east-1`).

Specify the handler and an existing role:

 - *Handler:* `com.amazonaws.services.lambda.RemoveAthenaPartitions::handleRequest`
 - *Existing role:* `lambda_athena_exec_role`

Set the timeout to one minute.



