from concurrent.futures import ThreadPoolExecutor, as_completed
from pyspark.sql import SparkSession

def write2parquet(start):
   #filter records only for 4 months 
   df=rdf.filter((rdf.month >= start) & (rdf.month <= start+3))
   df.repartition(*partitionby).write.partitionBy(partitionby).mode("append").parquet(output,compression=codec)

partitionby=['year','month','day']
output='/user/hadoop/elblogs_pq'
codec='snappy'
hivetablename='default.elb_logs_raw_native_part'

spark = SparkSession.builder.appName("Convert2Parquet").enableHiveSupport().getOrCreate()
rdf = spark.table(hivetablename)

futures=[]
pool = ThreadPoolExecutor(1)

for i in [1,5,9]:
   futures.append(pool.submit(write2parquet, i))
for x in as_completed(futures):
   pass

spark.stop()
