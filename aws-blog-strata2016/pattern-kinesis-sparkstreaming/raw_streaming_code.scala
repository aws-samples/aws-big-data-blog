import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.Seconds
import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, BasicAWSCredentials}
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.regions.RegionUtils
import org.apache.spark.streaming.kinesis.KinesisUtils
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import org.apache.spark.storage.StorageLevel
import org.apache.spark.SparkConf
import org.apache.spark.sql.types.{StructType,StructField,StringType}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.SaveMode.Append


val endpointUrl = "https://kinesis.us-east-1.amazonaws.com"
val credentials = new DefaultAWSCredentialsProviderChain().getCredentials()
    require(credentials != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")
    val kinesisClient = new AmazonKinesisClient(credentials)
    kinesisClient.setEndpoint("https://kinesis.us-east-1.amazonaws.com")
    val numShards = kinesisClient.describeStream("spark-demo").getStreamDescription().getShards().size

val numStreams = numShards
val batchInterval = Seconds(1)
val kinesisCheckpointInterval = batchInterval
val regionName = RegionUtils.getRegionByEndpoint(endpointUrl).getName()

val ssc = new StreamingContext(sc, Seconds(1))

 // Create the Kinesis DStreams
    val kinesisStreams = (0 until numStreams).map { i =>
      KinesisUtils.createStream(ssc, "app-spark-demo", "spark-demo", endpointUrl, regionName,InitialPositionInStream.LATEST, kinesisCheckpointInterval, StorageLevel.MEMORY_AND_DISK_2)
    }

    // Union all the streams
val unionStreams = ssc.union(kinesisStreams)

val schemaString = "d t ts"

val tableSchema = StructType( schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))

 unionStreams.foreachRDD ((rdd: RDD[Array[Byte]], time: Time) => {
  val rowRDD = rdd.map(w => Row.fromSeq(new String(w).split(",")))
  val wordsDF = sqlContext.createDataFrame(rowRDD,tableSchema)
  wordsDF.registerTempTable("realTimeTable")
}) 




//Start streaming context  (Another para)
ssc.start()


//Run all you %sql commands in Zeppelin here

//Gracefully stop streaming context  (another para)
ssc.stop(stopSparkContext=false, stopGracefully=true)
