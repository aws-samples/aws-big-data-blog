# Serverless S3 Metadata Index
The code in this directory accompanies the AWS Big Data Blog on Building and Maintaining an Amazon S3 Metadata Index without Servers.

## Contents

This subtree contains the following code samples:

- **example-indexer-app:** Example serverless application demonstrating how to implement an Amazon DynamoDB object index with AWS Lambda
- **s3-log-generator:** Java program used to generate dummy objects and upload them to S3 in order to test the index system.
- **query-examples:** Example scripts for querying the index

## Deploying the example application

To deploy the example application you must have the [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html) installed locally. Please follow the instructions on the linked page to complete the installation before continuing.

1. Clone this repository to your local machine.
1. Open a command prompt and change directories to the `example-indexer-app` subdirectory within `aws-blog-s3-index-with-lambda-ddb`.
1. Run `sam deploy --guided`.
1. When prompted, enter a unique name for the stack (e.g. "S3IndexerSample").
1. Enter the region you wish to deploy the application to.
1. Hit <Enter> to accept the defaults for each of the next options.
1. The SAM CLI should now deploy the application to your AWS account.

**After the deployment is complete you need to add an additional permission to the IAM role created for the indexer function**

1. Look up the physical name of the S3 bucket created by the CloudFormation stack using the CloudFormation management console.
    1. Select your stack.
    1. Select the resources tab.
    1. Copy the **Physical ID** value for the **BucketToIndex** logical resource.
1. Open the IAM management console.
1. Open the **Roles** list page from the left navigation menu.
1. Select the role with a name that follows the pattern **[Your Stack Name]-IndexerFunctionRole-[Unique ID]**.
1. Click the link to **Add inline policy** on the right side of the **Permissions policies** section.
1. Select the **JSON** tab.
1. Paste the following policy template into the input box and replace the [YOUR BUCKET NAME] place holder with the name of the bucket created by your stack.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::[YOUR BUCKET NAME]/*"
        }
    ]
}
```
1. Select **Review policy**.
1. Enter "S3Access" in the **Name** field.
1. Select **Create policy** to create the policy.

You can now test the system.

## Testing the system

You can now use the (log generator)[s3-log-generator] to add objects to the S3 buckets and test the system.

After you have added some objects you can refer to the (query examples)[query-examples] to see how to use the index to run the reports and analyses discussed in the post.
