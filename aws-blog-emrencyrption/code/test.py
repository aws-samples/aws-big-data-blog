import argparse
from pyspark.sql import SparkSession
from pyspark.sql import SQLContext

parser = argparse.ArgumentParser()
parser.add_argument("--bucketName", help="some useful desc")
args = parser.parse_args()

print args.bucketName

spark = SparkSession.builder \
    .appName("Python Spark SQL basic example") \
    .getOrCreate()

sc = spark.sparkContext
sqlContext = SQLContext(sc)

df = sqlContext.read.format('com.databricks.spark.csv').options(header='true', inferschema='true').load('s3://aws-bigdata-blog/artifacts/emr-encryption/data/cars/cars.csv')
df.select('year','model').write.format('com.databricks.spark.csv').option('header', 'true').save('s3://' + args.bucketName +'/encrypted-data/newcars.csv')
