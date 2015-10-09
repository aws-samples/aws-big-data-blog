# Node.js Streaming MapReduce with Amazon EMR

This is the code repository for code sample used in AWS Big data blog [Node.js Streaming MapReduce with Amazon EMR]

## Prerequisites 
  - Amazon Web Services account
  - [AWS Command Line Interface (CLI)]
 
Please refer to the blog post for detailed usage. [Node.js Streaming MapReduce with Amazon EMR] 

[AWS Command Line Interface (CLI)]:http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
[Node.js Streaming MapReduce with Amazon EMR]:http://blogs.aws.amazon.com/bigdata/post/Tx21KEQW59RMMGS/Node-js-Streaming-MapReduce-with-Amazon-EMR

## Run Sample Locally

We've provided ```test-local.sh``` which you can use to test local changes with one of the sample tweet files. To run it, simply run from a Terminal:

```
./test-local.sh
```

## Run Sample on EMR

Create a new Elastic MapReduce Cluster with Node.js and NPM installed:

```
aws emr create-cluster --ami-version 3.3.1 --enable-debugging --visible-to-all-users --name MyNodeJsMapReduceCluster --instance-groups  InstanceCount=2,InstanceGroupType=CORE,InstanceType=m3.xlarge InstanceCount=1,InstanceGroupType=MASTER,InstanceType=m3.xlarge --no-auto-terminate --enable-debugging --log-uri s3://<log bucket>/EMR/logs --bootstrap-actions Path=s3://elasticmapreduce/bootstrap-actions/node/install-nodejs.sh,Name=InstallNode.js --service-role EMR_DefaultRole --ec2-attributes KeyName=<my key pair>,InstanceProfile=EMR_EC2_DefaultRole
```
where:

* 'log bucket' is the S3 bucket into which the EMR Logfiles should placed
* 'my key pair' is the name of the EC2 key pair which should be used to access the EMR hosts

This will output a cluster ID, which will be used to add steps:

```
aws emr add-steps --cluster-id <my cluster ID> --steps Name=NodeJSStreamProcess,Type=Streaming,Args=--files,"s3://elasticmapreduce/samples/node/scripts/sample-mapper.js\,s3://elasticmapreduce/samples/node/scripts/sample-reducer.js",-input,s3://elasticmapreduce/samples/node/tweets,-output,s3://<my output bucket>/node_sample,-mapper,mapper.js,-reducer,reducer.js
```

Where 'my output bucket' is the name of the bucket where you would like output to be created. When completed, multiple files will reside in the configured output bucket and path, and will contain a rollup of the number of tweets for the single day in the sample data set: 

```{"day":"14 Feb 2013","count":1071}```