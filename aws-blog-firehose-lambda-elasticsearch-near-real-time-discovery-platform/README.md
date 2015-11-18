# Building near real-time discovery platform<br>with AWS Lambda, Amazon Kinesis Firehose and Elasticsearch


This is the code repository for code sample used in AWS Big data blog [Building a Near-Real-Time Discovery Platform with AWS]

## Prerequisites 
  - Amazon Web Services account
  - Elasticsearch cluster
  - [AWS Command Line Interface (CLI)]
  - [Node.js] installed
  - [Twitter application] with consumer key (API Key), consumer secret (API Secret), access token, and access token secret
 

## Overview of Example

### AWS Lambda function
AWS Lambda function (lambda-s3-twitter-to-es-python/lambda_function.py) that is triggered once a new file is created on S3. 
The function does the following:<br>
1.	Reads the file content<br>
2.	Parses the content to JSON format (Elasticsearch stores documents in JSON format)<br>
3.	Analyzes Twitter data (lambda-s3-twitter-to-es-python/tweet_utils.py):<br>
&nbsp;&nbsp;a.	Extracts Twitter mentions (@username) from the tweet text.<br>
&nbsp;&nbsp;b.	Extracts sentiment based on emoticons. If there’s no emoticon in the text the function uses [textblob sentiment analysis]<br>
4.	Loads the data to Elasticsearch (twitter_to_es.py) using [elasticsearch-py library]<br>



You can download and unzip the deployment package (packages/s3_twitter_to_es.zip), which already includes elasticsearch and textblob modules, or [create a deployment package yourself].<br>

Please replace ``<<PARAMETER>>`` with your values.

Modify s3-twitter-to-es-python/config.py by assigning es_host and es_port

Zip your deployment folder
```
cd path/to/s3-twitter-to-es-python
zip -r -9 ../s3-twitter-to-es-python.zip .
```

[create IAM Role] and name it ```<<LAMBDA_EXEC_ROLE>>```

create aws lambda function
```
aws lambda create-function \
--region <<REGION>> \
--function-name s3-twitter-to-es-python  \
--zip-file fileb://path/to/s3_twitter_to_es.zip \
--role arn:aws:iam::<<ACCOUNT_ID>>:role/<<LAMBDA_EXEC_ROLE>> \
--handler lambda_function.handler \
--runtime python2.7 \
--timeout 120
```

[Add S3 as the event source] to the lambda function with your ```<<S3_BUCKET>>``` and ```<<S3_PREFIX>>```



### Running example

Create Firehose IAM Role named “firehose_delivery_role” based on the following policy:

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "",
            "Effect": "Allow",
            "Action": [
                "s3:AbortMultipartUpload",
                "s3:GetBucketLocation",
                "s3:GetObject",
                "s3:ListBucket",
                "s3:ListBucketMultipartUploads",
                "s3:PutObject"
            ],
            "Resource": [
                "arn:aws:s3:::```<<S3_BUCKET>>```"
            ]
        }
    ]
}


setup nodejs application

```
cd firehose-twitter-streaming-nodejs
npm install
```

modify configurations in config.js

•	firehose<br>
&nbsp;&nbsp;o	DeliveryStreamName – name your stream. The app will create the delivery stream if it does not exist<br>
&nbsp;&nbsp;o	BucketARN: Use ```<<S3_BUCKET>>``` that you entered as the event source for the lambda function<br>
&nbsp;&nbsp;o	RoleARN: Use the Firehose role you created earlier (“firehose_delivery_role”)<br>
&nbsp;&nbsp;o	 Prefix: Use ```<<S3_PREFIX>>``` that you entered as the event source for the lambda function<br>
•	twitter – enter your Twitter application keys.<br>
•	region – your firehose region (e.g.: us-east-1, us-west-2, eu-west-1)<br>

run the application
```
node twitter_stream_producer_app
```

Please see [Building a Near-Real-Time Discovery Platform with AWS] for more details on using Elasticsearch and Kibana as the discovery platform.


[AWS Command Line Interface (CLI)]:http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
[textblob sentiment analysis]:http://textblob.readthedocs.org/en/dev/quickstart.html#sentiment-analysis
[elasticsearch-py library]:http://elasticsearch-py.readthedocs.org/en/master/
[create a deployment package yourself]:http://docs.aws.amazon.com/lambda/latest/dg/lambda-python-how-to-create-deployment-package.html
[create IAM Role]:http://docs.aws.amazon.com/lambda/latest/dg/walkthrough-s3-events-adminuser-create-test-function-create-execution-role.html
[Add S3 as the event source]:http://docs.aws.amazon.com/lambda/latest/dg/getting-started-2-integrate-s3events-console.html
[Node.js]:https://nodejs.org
[Twitter application]:https://apps.twitter.com/
[Building a Near-Real-Time Discovery Platform with AWS]:http://blogs.aws.amazon.com/bigdata/post/Tx1Z6IF7NA8ELQ9/Building-a-Near-Real-Time-Discovery-Platform-with-AWS



