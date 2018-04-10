# How to retain system tables’ data spanning multiple Amazon Redshift clusters and run cross-cluster diagnostic queries

## Overview

This repository contains the code that supports the [AWS Big Data Blog Post](https://blogs.aws.amazon.com/bigdata/)

### Usecase Description
Amazon Redshift is a warehouse service that logs the history of the system in STL log tables. The STL log tables manage disk space by retaining only two to five days of log history, depending on log usage and available disk space.

To retain STL tables’ data for an extended period, you would usually have to create a replica table for every system table and load the data from the system table in the replica at regular intervals. By maintaining replica tables for STL tables, you can run diagnostic queries on historical data from the STL tables. You then can derive insights from query execution times, query plans, and disk-spill patterns, and make better cluster-sizing decisions. However, refreshing replica tables with live data from STL tables at regular intervals requires schedulers such as Cron or AWS DataPipeline. Also, these tables are specific to one cluster and they are not accessible after the cluster is terminated. This is especially true for transient Amazon Redshift clusters that last for only a finite period of ad hoc query execution.  

In this blog post, I present a solution that exports system tables from multiple Amazon Redshift clusters into an Amazon S3 bucket. This solution is serverless and can be scheduled as frequently as every five minutes. The AWS CloudFormation deployment template I provide automates the solution setup in your environment. . The system tables’ data in the Amazon S3 bucket is partitioned by cluster name and date to enable efficient joins in cross-cluster diagnostic queries

Another CloudFormation template I provide later in this post helps to automate the creation of tables in the AWS Glue Data Catalog for the system tables’ data stored in Amazon S3. After the system tables are exported to Amazon S3, you can use Amazon QuickSight, Amazon Athena, Amazon EMR, or Amazon Redshift Spectrum to run cross-cluster diagnostic queries on the system tables’ data and derive insights about queries and cluster sizes.

### Common Errors
Information on resolving some of the common errors you may experience in this solution can be found [here](/Common_Errors.md)

### Cross Cluster Diagnostic Queries
Once you completed the deployment steps outlined in the blog in the sections: __Solution deployment__ and __Querying the exported sytem tables__, the Amazon Athena compatible cross cluster diagnostic queries can be found  [here](/cross_cluster_diagnostic_queries.md)

