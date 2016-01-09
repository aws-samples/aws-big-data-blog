# Setting up Apache Zeppelin on EC2 as a YARN client#
This is the code repository for samples used the AWS Big Data blog [Running an External Zeppelin Instance using S3 Backed Notebooks with Spark on Amazon EMR](http://blogs.aws.amazon.com/bigdata/post/Tx2HJD3Z74J2U8U/Running-an-External-Zeppelin-Instance-using-S3-Backed-Notebooks-with-Spark-on-Am)

###Overview of Example###

This repository contains a CloudFormation script that bootstraps an EC2 instance with Apache Zeppelin, configures the instance as a YARN client of an EMR cluster, and configures Zeppelin to store notebooks in S3. Please see the blog post of the same name for configuration instructions.

**Important:** These resources will incur charges on your AWS bill. It is your responsbility to delete these resources.
