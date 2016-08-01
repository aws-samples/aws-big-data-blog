## Synopsis

Install instructions for the blog post - https://blogs.aws.amazon.com/bigdata/post/Tx3RS3V80XNRJH3/Real-time-in-memory-OLTP-and-Analytics-with-Apache-Ignite-on-AWS

## Install

1) sudo yum install git

2) git clone https://github.com/awslabs/aws-big-data-blog.git

3) cd aws-big-data-blog/aws-blog-real-time-in-memory-oltp-and-analytics-with-apache-ignite

## Run sample code to push data to DynamoDB

vim sample/dummyOrderGenerator.py

--Modify the below two lines in the python code with your region and DynamoDB table name 
conn = boto.dynamodb.connect_to_region('<Region_Name>')
table = conn.get_table('<DynamoDB_Table_Name>')

## Create Lambda function using this sample code to process ddb streams

vim sample/ddbStreamstoFirehose.py

--Modify the below line in the python function and deploy it to Lambda
DeliveryStreamName='<Firehose_Delivery_Stream_Name>'

## Modify KCL code with your endpoint information

cd bin
vim src/main/java/com/amazon/dynamostreams/clientlibrary/AmazonDynamoDBStreamstoIgnite.java

--Modify these lines with your information
private static String streamsEndpoint = "streams.dynamodb.us-east-1.amazonaws.com";
private static String streamArn = "arn:aws:dynamodb:us-east-1:349905090898:table/OrderDetails/stream/2016-01-16T21:59:00.129";
private static String dynamodbEndpoint = "dynamodb.us-east-1.amazonaws.com";

--Modify this with your Ignite cluster IP endpoints
ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509","<Ignite-Node-IP>:47500..47509","<Ignite-Node-IP>:47500..47509"));

--Modify with your Ignite cache Name
IgniteDataStreamer<String, orderdata> cache = Ignition.ignite().dataStreamer("<Cache Name>");

## Compile
mvn clean && mvn install


