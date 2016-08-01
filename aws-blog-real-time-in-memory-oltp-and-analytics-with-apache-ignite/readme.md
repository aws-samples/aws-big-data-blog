## Synopsis

Install instructions for the blog post - https://blogs.aws.amazon.com/bigdata/post/Tx3RS3V80XNRJH3/Real-time-in-memory-OLTP-and-Analytics-with-Apache-Ignite-on-AWS

## Install

1) sudo yum install git

2) git clone https://github.com/awslabs/aws-big-data-blog.git

3) cd aws-big-data-blog/aws-blog-real-time-in-memory-oltp-and-analytics-with-apache-ignite

## Run sample code to push data to DynamoDB

In ```sample/dummyOrderGenerator.py```, modify the below two lines in the python code with your region and DynamoDB table name 

```
conn = boto.dynamodb.connect_to_region('<Region_Name>')

table = conn.get_table('<DynamoDB_Table_Name>')
```

## Create Lambda function using this sample code to process ddb streams

In ```sample/ddbStreamstoFirehose.py```, modify the below line in the python function and deploy it to Lambda:

```
DeliveryStreamName='<Firehose_Delivery_Stream_Name>'
```

## Modify KCL code with your endpoint information

In ```src/main/java/com/amazon/dynamostreams/clientlibrary/StreamsToIgnite.properties```, modify these lines with your information:

```
streamsEndpoint = streams.dynamodb.<region>.amazonaws.com
streamArn = arn:aws:dynamodb:<region>:<my-account>:table/OrderDetails/stream/2016-01-16T21:59:00.129
dynamodbEndpoint = dynamodb.region.amazonaws.com

--Modify this with your Ignite cluster IP endpoints
hostList = 127.0.0.1:47500..47509,54.152.85.31:47500..47509,52.91.24.72:47500..47509

-- Modify with your Ignite cache Name
cacheName = dynamocache

-- kinesis settings
applicationName = ddbstreamsprocessing
maxRecords = 1000
initialPositionInStream = LATEST
```

## Compile
```mvn clean && mvn install```


