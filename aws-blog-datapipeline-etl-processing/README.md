# ETL Processing using AWS DataPipeline and Amazon EMR

This is the code repository for code sample used in AWS Big data blog [ETL Processing using AWS Datapipeline and Amazon EMR]

## Prerequisites 
  - Amazon Web Services account
  - [AWS Command Line Interface (CLI)]
 

#### Overview of Example
Creating an ETL Pipeline to process web server logs which are available in S3 bucket. 
Pipeline multi step:
* Check if log files are available in the S3 bucket 
* Create an EMR cluster with EMRFS on it
* Run emrfs sync to update metadata with contents of S3 bucket
* Submit a Pig job on EMR cluster as step
* Reuse the same EMR cluster to launch a Hive job for generating reports
* Clean EMRFS metadata that is older than 2 days (emrfs delete)

### Running example

You can download the sample myWorkflow.json used in this example and create a pipeline using the AWS Universal CLI. This pipeline will run once for a day in the past. Before you create the pipeline, please replace ``<<MY_BUCKET>>`` and ``<<MY_KEYPAIR>>`` values with your S3 bucket and keypair. 

Creating the pipeline is simple using create-pipeline.  Output of this command will be pipeline id. 

```
$ aws datapipeline create-pipeline --name myETLWorkflow --unique-id myETLWorkflow
 {
     "pipelineId": "df-XXXXXXXXXXX "
 }
 ```

Next we put the pipeline definition. If there are any errors/warnings in your JSON it will be printed on screen. Creating the pipeline does not schedule it for execution. 

```
$ aws datapipeline put-pipeline-definition --pipeline-id df-XXXXXXXXXXX --pipeline-definition file://myWorkflow.json
```

You need to activate the pipeline before it can run on the schedule defined.
```
$ aws datapipeline activate-pipeline --pipeline-id df-XXXXXXXXXXX
```

[AWS Command Line Interface (CLI)]:http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
[ETL Processing using AWS Datapipeline and Amazon EMR]:http://blogs.aws.amazon.com/bigdata/post/Tx1PU7JM7I34L81/ETL­-Processing-Using-AWS-Data-Pipeline-and-Amazon-Elastic-MapReduce
