# Converting a large dataset to Parquet

This is a Spark script that can read data from a Hive table and convert the dataset to the Parquet format. We will be using a combination of Spark and Python native threads to convert a 1 TB CSV dataset to Parquet in batches. Our sample dataset is 1 year of ELB log data in S3 available as a Hive External Table, and we will be converting this dataset to Parquet in 3 batches, each batch comprising of 4 months of data. As we are reading the data from a Hive Table, we can use this script to convert CSV, TSV, JSON or any Hive supported format to Parquet or ORC files.

## Prerequisites
- Amazon Web Services account
- [AWS Command Line Interface (CLI)]

#### Launching an AWS EMR Spark cluster

You can either launch a Spark cluster from the AWS console or using the AWS CLI as below. For this purpose, we will be selecting a cluster of 2 r3.8xlarge nodes. This will ensure that the memory avalable to the cluster will be greater than 333 GB to accomodate 4 months of data in each run. We will also be adding EBS storage volumes to ensure we can accomodate the intermediate and output data in HDFS. Please ensure that the EMR Master Security Group allows you SSH access.

```
aws emr create-cluster --applications Name=Hadoop Name=Hive Name=Spark Name=Tez \
--ec2-attributes '{  
  "KeyName":"<<KEY NAME>>",
  "InstanceProfile":"EMR_EC2_DefaultRole",
  "SubnetId":"<<SUBNET ID>>",
  "EmrManagedSlaveSecurityGroup":"<<SECURITY GROUP ID>>",
  "EmrManagedMasterSecurityGroup":"<<SECURITY GROUP ID>>"
}'  \
--service-role EMR_DefaultRole --release-label emr-5.2.0 \
--name 'Spark Cluster' --instance-groups '[  
  {  
    "InstanceCount":1,
    "InstanceGroupType":"MASTER",
    "InstanceType":"m3.xlarge",
    "Name":"Master instance group - 1"
  },
  {  
    "InstanceCount":2,
    "EbsConfiguration":{  
      "EbsBlockDeviceConfigs":[  
        {  
          "VolumeSpecification":{  
            "SizeInGB":840,
            "VolumeType":"gp2"
          },
          "VolumesPerInstance":1
        }
      ],
      "EbsOptimized":false
    },
    "InstanceGroupType":"CORE",
    "InstanceType":"r3.8xlarge",
    "Name":"Core instance group - 2"
  }
]'  \
--region us-east-1
```

##### Creating SSH Tunnel to the EMR Master Node
```
Local $> ssh -o ServerAliveInterval=10 -i <<credentials.pem>> -N -D 8157 hadoop@<<master-public-dns-name>>
```

### Running the sample script

We will SSH to the master node and create the Hive table and submit the spark job. Execute the DDL to create the Hive External table in Hive, and then copy the script convert2.parquet.py to the master node. Spark Executors are distributed agents that execute Spark tasks in parallel. For this example, we will be allocating 85 executors with 5 GB memory each to process the data.
```
Local $> ssh -i <<credentials.pem>> hadoop@<<master-public-dns-name>>
EMR   $> hive -f createtable.hql
EMR   $> hive -f addpartitions.hql
EMR   $> spark-submit  --num-executors 85  --executor-memory 5g convert2parquet.py
```

### Script Overview 
Reading the Hive Table into a Spark DataFrame.
```
hivetablename='default.elb_logs_raw_part'
spark = SparkSession.builder.appName("Convert2Parquet").enableHiveSupport().getOrCreate()
rdf = spark.table(hivetablename)
```

Writing dataframe to Parquet
```
codec='snappy'
df.repartition(*partitionby).write.partitionBy(partitionby).mode("append") \
   .parquet(output,compression=codec)
```

The script can be eaily changed to write the final output in ORC files instead of Parquet.
```
codec='zlib'
df.repartition(*partitionby).write.partitionBy(partitionby).mode("append") \
  .orc(output,compression=codec)
```

The script uses Python native threads to orchestrate each batch of 4 months. This ensures that we read only enough data in each batch that fits into the memory of the cluster. Our Python thread pool has only 1 thread to process one batch at any one time, This could have been made larger to process batches in parallel on larger clusters.
```
futures=[]
pool = ThreadPoolExecutor(1)
futures.append(pool.submit(write2parquet, i))

```

### Copying the output back to S3. 

We use s3-dist-cp to copy the output back to S3. You can then define a Hive external table over this data.
```
EMR $> s3-dist-cp --src="hdfs:///user/hadoop/elblogs_pq" --dest="s3://<<BUCKET>>/<<PREFIX>>" 
```
