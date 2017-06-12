# Building a graph database on AWS using Amazon DynamoDB and Titan

## Overview

This repository contains the code that supports the [AWS Big Data Blog post](https://blogs.aws.amazon.com/bigdata/) of the same name.

The code is a single java class that acts as a factory for loading the sample data.  The sample data is included as resource files in the projects.  The data used in this sample was based on a [data set](https://archive.ics.uci.edu/ml/datasets/Restaurant+%26+consumer+data) provided at the Machine Learning Repository at UCL (Blanca Vargas-Govea, Juan Gabriel González-Serna, Rafael Ponce-Medellín. Effects of relevant contextual features in the performance of a restaurant recommender system. In RecSys’11: Workshop on Context Aware Recommender Systems (CARS-2011), Chicago, IL, USA, October 23, 2011).

[Maven](https://maven.apache.org/) is used for building and dependency management.


## Getting Started
This example populates a Titan graph database backed by DynamoDB Local using the sample data discussed in the blog post.

### Build the Titan Restaurants Factory

1. Clone the repository in GitHub.

    ```
    git clone https://github.com/awslabs/aws-big-data-blog.git
    ```
2. Navigate to the aws-blog-titan-graph-database directory

3. Run the `package` target to build the factory jar.

    ```
    mvn package
    ```
### Start a Titan graph database backed by DynamoDB Local

4. Clone the 'Amazon DynamoDB Storage Backend for Titan' repository in GitHub.

    ```
    git clone https://github.com/awslabs/dynamodb-titan-storage-backend.git
    ```
5. Run the `install` target to copy some dependencies to the target folder.

    ```
    mvn install
    ```
6. Copy the jar you built in step 3 to the './target/dependencies' directory

7. Start DynamoDB Local in a different shell.

    ```
    mvn test -Pstart-dynamodb-local
    ```
4. Run the Gremlin shell.

    ```
    mvn test -Pstart-gremlin
    ```
5. Open a graph using the Titan DynamoDB Storage Backend in the Gremlin shell.

    ```
    conf = new BaseConfiguration()
    conf.setProperty("storage.backend", "com.amazon.titan.diskstorage.dynamodb.DynamoDBStoreManager")
    conf.setProperty("storage.dynamodb.client.endpoint", "http://localhost:4567")
    conf.setProperty("index.search.backend", "elasticsearch")
    conf.setProperty("index.search.directory", "/tmp/searchindex")
    conf.setProperty("index.search.elasticsearch.client-only", "false")
    conf.setProperty("index.search.elasticsearch.local-mode", "true")
    conf.setProperty("index.search.elasticsearch.inteface", "NODE")
    g = TitanFactory.open(conf)
    com.amazonaws.bigdatablog.titanrestaurants.RestaurantFactory.load(g)
    ```
