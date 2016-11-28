# Converting a large dataset to Parquet

This is a Spark script that can read data from a Hive table and convert the dataset to the Parquet format. We will be using a combination of Spark and Python native threads to convert a 1 TB CSV dataset to Parquet in batches. Our sample dataset is 1 year of ELB log data in S3, and we will be converting this dataset to Parquet in 3 batches, each batch comprising of 4 months of data. As we are reading the data from a Hive Table, we can use this script to convert CSV, TSV, JSON or any Hive supported format to Parquet or ORC files.

## Prerequisites
- Amazon Web Services account
- [AWS Command Line Interface (CLI)]

#### Launching cluster

You can either launch a Spark cluster from the console or using the cli as below. For this purpose, we will be selecting a cluster of 2 r3.8xlarge nodes. The memory of the cluster will be greater than 333 GB to accomodate 4 months of data in each run. We will also be adding EBS Storage to ensure we can accomodate the output data in HDFS.

```
aws emr create-cluster --applications Name=Hadoop Name=Hive Name=Spark Name=Ganglia Name=Tez \
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

##### Creating SSH Tunnel
```
ssh -o ServerAliveInterval=10 -i <<credentials.pem>> -N -L 8192:<<master-public-dns-name>>:8192 hadoop@<<master-public-dns-name>>
```

### Running the example
We will SSH to the master node and submit the spark job. Copy the script convert2.parquet.py to the master node.
Spark Executors are distributed agents that execute tasks. For this example, we will be allocating 85 executors with 5 GB memory each to process each batch of data.
```
spark-submit  --num-executors 85  --executor-memory 5g convert2parquet.py
```

#### Overview of Script
Reading the Hive Table into a Spark DataFrame.
```
hivetablename='default.elb_logs_raw'
sc = SparkContext(conf=conf)
hive_context = HiveContext(sc)
rdf = hive_context.table(hivetablename)
```

Converting to Parquet
```
codec='snappy'
df.repartition(*partitionby).write.partitionBy(partitionby).mode("append").parquet(output,compression=codec)
```

This could have been changed to write the final output in ORC instead of Parquet.
```
codec='zlib'
df.repartition(*partitionby).write.partitionBy(partitionby).mode("append").orc(output,compression=codec)
```

Using Python native threads to orchestrate each batch of 4 months. Our thread pool has only 1 thread, this could have been made larger to process batches in parallel.
```
futures=[]
pool = ThreadPoolExecutor(1)
futures.append(pool.submit(write2parquet, i))

```

Copying the output back to S3. You can then define a Hive external table over this data.
```
s3-dist-cp --src="hdfs:///user/hadoop/elblogs_pq" --dest="s3://<<BUCKET>>/<<PREFIX>>" 
```

