# Real-time Clickstream Analysis with Amazon Kinesis Analytics
 
This is the code repository for code sample used in AWS Big data blog [Real-time Clickstream Analysis with Amazon Kinesis Analytics]

## Prerequisites 
  - Amazon Web Services account
  - Python  

#### Overview of Example
Aalytics Pipeline to process web beacon requests.
Pipeline:
* Python script to generate simulated browser requests. 
* API Gateway to accept requests and send them as JSON messages to a Kinesis stream
* Kinesis stream used as the import stream
* Kiniesis Analytics application to process the messages and generate an anomaly score
* Kinesis stream to accept the output of the Kinesis Analytics application
* Lambda function to react to records identified as anomalies
* SNS Topic to send messages to when anomalies are detected

### Running example

* Create a stack in CloudFormation using CFE-cfn.json. 
* Copy ClickImpressionGenerator.py to a local folder.
* If you do not have requests globally installed in ths same folder run 
```
pip install requests -t .
```
* When the CloudFormation stack is complete select the ExecuteCommand from the Output of the stack and run that in the folder with ClickImpressionGenerator.py to start generating data. 
* Create a Kinesis Analytics application selecting <stack name>-CSEKinesisBeaconInputStream-<random string> for the input stream and <stack name>-CSEKinesisAnalyticsRole-<random string> for the existing role.
* Paste the contents of CSE-SQL.sql to the Kinesis Analytics code window.
* Select <stack name>-CSEKinesisBeaconOutputStream-<random string> for the input stream and <stack name>-CSEKinesisAnalyticsRole-<random string> for the existing role.
