# Large-Scale Machine Learning with Spark on Amazon EMR
This is the code repository for the code sample used in the AWS Big Data blog post Large-Scale Machine Learning with 
Spark on Amazon EMR.  It demonstrates an example machine learning workflow using Spark and MLlib on EMR.

## Prerequisites
  - Amazon Web Services account
  - [AWS Command Line Interface (CLI)](http://aws.amazon.com/cli/)
  - [sbt](http://www.scala-sbt.org/)
  - [sbt-assembly](https://github.com/sbt/sbt-assembly)
  
## Building
```
sbt assembly
```

## Copying to S3
```
aws s3 cp spark-emr/target/scala-2.10/spark-emr-assembly-1.0.jar s3://your-bucket-name/$USER/spark/jars/spark-emr-assembly-1.0.jar
```

## Example invocation

```
aws emr create-cluster \
  --name "exampleJob" \
  --ec2-attributes KeyName=MyKeyName \
  --auto-terminate \
  --ami-version 3.8.0 \
  --instance-type m3.xlarge \
  --instance-count 3 \
  --log-uri s3://your-bucket-name/$USER/spark/`date +%Y%m%d%H%M%S`/logs \
  --applications Name=Spark,Args=[-x] \
  --steps "Name=\"Run Spark\",Type=Spark,Args=[--deploy-mode,cluster,--master,yarn-cluster,--conf,spark.executor.extraJavaOptions=-XX:MaxPermSize=256m,--conf,spark.driver.extraJavaOptions=-XX:MaxPermSize=512m,--class,ModelingWorkflow,s3://your-bucket-name/$USER/spark/jars/spark-emr-assembly-1.0.jar,s3://support.elasticmapreduce/bigdatademo/intentmedia/,s3://your-bucket-name/$USER/spark/output/]"
```
