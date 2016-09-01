This is an example lab on how to do spark streaming with Zeppelin on EMR

Overall arch:

Sensors data (is pushed from Kinesis stream) -> Kinesis (stream name spark-demo) -> Spark Streaming on EMR -> Visualize using Zeppelin


1) Launch an EC2 instance and run the Kinesis Producer jar. It will pushed data into Kinesis stream named "spark-demo". Based on the size of instance and kinesis stream the number of events will automatically grow. I have tested it with 50K events/sec (using c3.4xl and 50 shards on Kinesis)
  Built using Kinesis Producer Library
		https://github.com/awslabs/aws-big-data-blog/tree/master/aws-blog-kinesis-producer-library

2) Spin up an EMR cluster with Spark+Zeppelin.
   Add this line to zeppelin-env.sh   (at /etc/zeppelin/conf )

    export SPARK_SUBMIT_OPTIONS="$SPARK_SUBMIT_OPTIONS  --packages org.apache.spark:spark-streaming-kinesis-asl_2.10:1.6.0 --conf spark.executorEnv.PYTHONPATH=/usr/lib/spark/python/lib/py4j-0.9-src.zip:/usr/lib/spark/python/:<CPS>{{PWD}}/pyspark.zip<CPS>{{PWD}}/py4j-0.9-src.zip --conf spark.yarn.isPython=true"


3) Use the JSON file here - it is a zeppelin notebook. 
4) Enjoy!!

