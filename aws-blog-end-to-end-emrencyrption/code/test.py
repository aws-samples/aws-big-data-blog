from pyspark.sql import SparkSession

spark = SparkSession.builder \
    .appName("Python Spark SQL basic example") \
    .getOrCreate()

sc = spark.sparkContext

df = sqlContext.read.format('com.databricks.spark.csv').options(header='true', inferschema='true').load('s3://aws-sai-sriparasa/datalake/emr-encrypt/cleartext/cars.csv')
df.select('year','model').write.format('com.databricks.spark.csv').option('header', 'true').save('s3://your-bucket/encrypted-data/newcars.csv')
