// scalastyle:off println
// AUTHOR: Amo Abeyaratne
// DATE Updated : 10-sep-2015
// Company : AWS Sydney
// This is built as a proof of conecpt to show how spark streaming can be used for representing micro-batched data as temporary tables via JDBC.

package org.apache.spark.examples.streaming

import java.nio.ByteBuffer

import scala.util.Random

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, BasicAWSCredentials}
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.amazonaws.services.kinesis.model.PutRecordRequest
import org.apache.log4j.{Level, Logger}

import org.apache.spark.{Logging, SparkConf}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Milliseconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream.toPairDStreamFunctions
import org.apache.spark.streaming.kinesis.KinesisUtils

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Time, Seconds, StreamingContext}
import org.apache.spark.util.IntParam
import org.apache.spark.sql.SQLContext


import org.apache.spark.storage.StorageLevel

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.hive.thriftserver.HiveThriftServer2

// Import Row.
import org.apache.spark.sql.Row;

// Import Spark SQL data types
import org.apache.spark.sql.types.{StructType,StructField,StringType}
import org.apache.spark.sql.SaveMode.Append


object KinesisWatch extends Logging {
  def main(args: Array[String]) {
    // Check that all required args were passed in.
    if (args.length != 7) {
      System.err.println(
        """
          |Usage: KinesisWatch <app-name> <stream-name> <endpoint-url> <batch-length-ms> <schmaString> <inputDelimiter> <tempTableName>
          |
          |    <app-name> is the name of the consumer app, used to track the read data in DynamoDB
          |    <stream-name> is the name of the Kinesis stream
          |    <endpoint-url> is the endpoint of the Kinesis service
          |                   (e.g. https://kinesis.us-east-1.amazonaws.com)
          |    <batch-length-ms> How long is each batch for the temp table view directly from stream in milliseconds
          |    <schmaString>  What should be the schema for CSV inputs via kinesis. Block within double quotes.
          |                   i.e "custid date value1 value2" for a 4 column dataframe. This has to match the input data.
          |    <inputDelimiter> Delimiter? - how to split the data in each row of the stream to generate fields i.e ","
          |    <tempTableName> Name of the tempTable accessible via JDBC HiveThriftServer2 for the duration of the batch. 
          |     
        """.stripMargin)
      System.exit(1)
    }

    //StreamingExamples.setStreamingLogLevels()

    // Populate the appropriate variables from the given args
    val Array(appName, streamName, endpointUrl, batchLength, schemaString, inputDelimiter, tempTableName ) = args


    // Determine the number of shards from the stream using the low-level Kinesis Client
    // from the AWS Java SDK.
    val credentials = new DefaultAWSCredentialsProviderChain().getCredentials()
    require(credentials != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")
    val kinesisClient = new AmazonKinesisClient(credentials)
    kinesisClient.setEndpoint(endpointUrl)
    val numShards = kinesisClient.describeStream(streamName).getStreamDescription().getShards().size


    // In this example, we're going to create 1 Kinesis Receiver/input DStream for each shard.
    // This is not a necessity; if there are less receivers/DStreams than the number of shards,
    // then the shards will be automatically distributed among the receivers and each receiver
    // will receive data from multiple shards.
    val numStreams = numShards

    // Spark Streaming batch interval
    val batchInterval = Milliseconds(batchLength.toLong)

    // Kinesis checkpoint interval is the interval at which the DynamoDB is updated with information
    // on sequence number of records that have been received. Same as batchInterval for this
    // example.
    val kinesisCheckpointInterval = batchInterval

    // Get the region name from the endpoint URL to save Kinesis Client Library metadata in
    // DynamoDB of the same region as the Kinesis stream
    val regionName = RegionUtils.getRegionByEndpoint(endpointUrl).getName()

    // Setup the SparkConfig and StreamingContext
    val sparkConfig = new SparkConf().setAppName("KinesisWatch")
    val ssc = new StreamingContext(sparkConfig, batchInterval)

    // Create the Kinesis DStreams
    val kinesisStreams = (0 until numStreams).map { i =>
      KinesisUtils.createStream(ssc, appName, streamName, endpointUrl, regionName,
        InitialPositionInStream.LATEST, kinesisCheckpointInterval, StorageLevel.MEMORY_AND_DISK_2)
    }

    // Union all the streams
    val unionStreams = ssc.union(kinesisStreams)

    val hiveContext = new HiveContext(ssc.sparkContext);
    
    
    //define the schema for input data structure
    //val schemaString = "datetimeid customerid grosssolar netsolar load controlledload"
    //Passing the schemaString as a part of the arguments seperated by spaces.
    
    val tableSchema = StructType( schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))


    //looping through the DStream (from Kinesis)
   
    unionStreams.foreachRDD ((rdd: RDD[Array[Byte]], time: Time) => {

    // Split each line in each Dstream RDD by given delimiter and feed

    val rowRDD = rdd.map(w => Row.fromSeq(new String(w).split(inputDelimiter)))  

    //create a DataFrame - For each DStream -> RDD for the batch window. This will basically convert a window of 
    //"batchInterval" size block of data on kinesis in to a dataframe.

    val wordsDataFrame = hiveContext.createDataFrame(rowRDD, tableSchema)

      // Register the current dataFrame in the loop as table
    wordsDataFrame.registerTempTable(tempTableName)

    // save the table Also as a persistent table in Hive. 

    wordsDataFrame.saveAsTable("permKinesisTable",Append)
//    wordsDataFrame.write.mode(Append).saveAsTable("permKinesisTable");

      // once the above is done. A table called "inputStream"

      val sqlString = "select count(*) from "+tempTableName
      // Do a count on table using SQL and print it for each timestamp on console
      val wordCountsDataFrame = hiveContext.sql(sqlString)
      
      println(s"========= $time =========")
      wordCountsDataFrame.show()
    })



// creating a HiveContext to present via JDBC - connect via JDBC /Beeline to test it.
HiveThriftServer2.startWithContext(hiveContext)

    // Start the streaming context and await termination - CTRL+C to terminate on console.

    ssc.start()
    ssc.awaitTermination()
  }
}



