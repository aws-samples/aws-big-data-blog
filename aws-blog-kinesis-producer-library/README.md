# Implementing Efficient and Reliable Producers with the Amazon Kinesis Producer Library

## Overview

This repository contains the code samples used in the [AWS Big Data Blog post](https://blogs.aws.amazon.com/bigdata/post/Tx3ET30EGDKUUI2/Implementing-Efficient-and-Reliable-Producers-with-the-Amazon-Kinesis-Producer-L) of the same name.

The code contains a single java package with various implementations of the base class `AbstractClickEventsToKinesis` as described in the blog post. There is a driver class called `ClickEventsToKinesisTestDriver` that can be executed to test out the implementations.

[Maven](https://maven.apache.org/) is used for building and dependency management.

## Running the Sample

You'll need to first create a stream to put data into. This stream should have a sufficient number of shards if you wish to try out the high-throughput implementations.

You can use any stream name and region, but you'll have to modify `AbstractClickEventsToKinesis` to use the name and region you have chosen.

To execute the test driver:

```
mvn compile exec:java -Dexec.mainClass=com.amazonaws.services.kinesis.producer.demo.ClickEventsToKinesisTestDriver
```

You should see output that looks like this:

```
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ aws-blog-kinesis-producer-library ---
0 seconds, 0 records total, 0.00 RPS (avg last 10s)
1 seconds, 0 records total, 0.00 RPS (avg last 10s)
2 seconds, 4 records total, 0.40 RPS (avg last 10s)
3 seconds, 7 records total, 0.70 RPS (avg last 10s)
4 seconds, 12 records total, 1.20 RPS (avg last 10s)
5 seconds, 16 records total, 1.60 RPS (avg last 10s)
6 seconds, 22 records total, 2.20 RPS (avg last 10s)
7 seconds, 28 records total, 2.80 RPS (avg last 10s)
8 seconds, 31 records total, 3.10 RPS (avg last 10s)
9 seconds, 36 records total, 3.60 RPS (avg last 10s)
10 seconds, 46 records total, 4.60 RPS (avg last 10s)
11 seconds, 55 records total, 5.50 RPS (avg last 10s)
...
```

To try out a different implementation of the producer, find and change the following line in `ClickEventsToKinesisTestDriver`:

```java
// Change this line to use a different implementation
final AbstractClickEventsToKinesis worker = new BasicClickEventsToKinesis(events);
```
