package com.awsproserv.kafkaandsparkstreaming

import kafka.serializer.StringDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{ Seconds, StreamingContext, Time }

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._


object ClickstreamSparkstreaming {

  def main(args: Array[String]) {

    if (args.length < 2) {
      System.err.println(s"""
        |Usage: ClickstreamSparkstreaming <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, topics) = args
    
    val sparkConf = new SparkConf().setAppName("DirectKafkaClickstreams")
    // Create context with 10 second batch interval
    val ssc = new StreamingContext(sparkConf, Seconds(10))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    val lines = messages.map(_._2)
    
    val warehouseLocation = "file:${system:user.dir}/spark-warehouse"

    val spark = SparkSession
      .builder
      .config(sparkConf)
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .enableHiveSupport()
      .getOrCreate()
   
    // Drop the tables if it already exists 
    spark.sql("DROP TABLE IF EXISTS csmessages_hive_table")
   
    // Create the tables to store your streams 
    spark.sql("CREATE TABLE csmessages_hive_table ( recordtime string, eventid string, url string, ip string ) STORED AS TEXTFILE")

    // Convert RDDs of the lines DStream to DataFrame and run SQL query
    lines.foreachRDD { (rdd: RDD[String], time: Time) =>
      
      import spark.implicits._
      // Convert RDD[String] to RDD[case class] to DataFrame
       
      val messagesDataFrame = rdd.map(_.split(",")).map(w => Record(w(0), w(1), w(2), w(3))).toDF()
      
      // Creates a temporary view using the DataFrame
      messagesDataFrame.createOrReplaceTempView("csmessages")
      
      //Insert continuous streams into hive table
      spark.sql("insert into table csmessages_hive_table select * from csmessages")

      // select the parsed messages from table using SQL and print it (since it runs on drive display few records)
      val messagesqueryDataFrame =
      spark.sql("select * from csmessages")
      println(s"========= $time =========")
      messagesqueryDataFrame.show()
    }

    // Start the computation
    ssc.start()
    ssc.awaitTermination()

  }
}
/** Case class for converting RDD to DataFrame */
case class Record(recordtime: String,eventid: String,url: String,ip: String)