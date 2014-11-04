# Kinesis Storm Clickstream Sample Application

The **Kinesis Storm Clickstream Sample Application** demonstrates how to use [Apache Storm][storm] to interact with [Amazon Kinesis][kinesis] and generate meaningful statistics and visualize the results in real time.

## Features

The Kinesis Storm Clickstream Sample Application contains the components described below

1. A record publisher, [HttpReferrerStreamWriter.java][publisher] to send data to [Amazon Kinesis][kinesis].
2. An [Apache Storm][storm] topology to compute over a sliding window and store the results in [Amazon Elasticache][elasticache].
3. A [Node.Js][node] server to publish the results as they are computed to a real time chart built using [Epoch][epoch].

## Getting Started
**Using the Amazon Kinesis Data Visualization Sample Application** &mdash; The best way to familiarize yourself with the sample application is to read the [Amazon Kinesis and Apache Storm: Building a Real-Time Sliding-Window Dashboard over Streaming Data][whitepaper] whitepaper. 

This application also includes a [CloudFormation][cloudformation] template at ```/static/cloudformation/kinesis-storm-clickstream-sample-app.template``` to launch the sample application on AWS. See [Launching using CloudFormation Template](#launching-using-cloudformation-template) for more information.

### Building and Deplyoing Storm Topology

You can build the sample application using [Maven][maven]:

```
mvn scm:bootstrap
mvn clean install
```

The application and all its dependencies are packaged into ```target/kinesis-storm-clickstream-sample-app-1.0.0-jar-with-dependencies.jar```. The package can then be deployed as a topology on a Apache Storm cluster.

```storm jar kinesis-storm-clickstream-sample-app-1.0.0-jar-with-dependencies.jar KinesisStormClickstreamApp.SampleTopology sample.properties RemoteMode```

### Starting the Real Time Chart Visualization

You can starts a [Node.JS][node] server to view the visualization in real time, by running:

```node node-app.js ElasticCacheRedisEndpoint```

The script is available at ```static/visualization/node-app.js```. You'll also need to install the [Epoch][epoch] library in the same directory. After you have deployed all the components, and started the Node server navigate to the port 9000 to view the chart.

## Launching using CloudFormation Template

A sample CloudFormation template is included to demonstrate how to launch the application on EC2. The template will create one Amazon Kinesis stream with two shards, an Amazon ElastiCache cluster, three Amazon EC2 instances in your AWS account and starts all the applications on them. The CloudFormation stack also creates an [IAM Role][iam-role] to allow the application to authenticate your account without the need for you to provide explicit credentials. See [Using IAM Roles for EC2 Instances with the SDK for Java][iam-roles-java-sdk] for more information.

Visit the [AWS CloudFormation][cloudformation] page for more information on what CloudFormation is and how you can leverage it to create and manage AWS resources.

Important: These resources will incur charges on your AWS bill. It is your responsibility to delete these resources.

[kinesis]: http://aws.amazon.com/kinesis
[elasticache]:http://aws.amazon.com/elasticache/
[publisher]: https://github.com/awslabs/amazon-kinesis-data-visualization-sample/blob/master/src/main/java/com/amazonaws/services/kinesis/samples/datavis/HttpReferrerStreamWriter.java
[storm]: https://storm.apache.org/
[node]: http://nodejs.org/
[epoch]: http://fastly.github.io/epoch/
[whitepaper]: http://d0.awsstatic.com/whitepapers/building-sliding-window-analysis-of-clickstream-data-kinesis.pdf
[cloudformation]: http://aws.amazon.com/cloudformation
[ec2-instance-types]: http://aws.amazon.com/ec2/instance-types
[iam-role]: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html
[iam-roles-java-sdk]: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-roles.html
[maven]: http://maven.apache.org/
