package com.amazonaws.proserv.blog

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}
import org.joda.time.DateTime

/**
  * Created by dgraeber on 5/2/2016.
  */
object FlightsBatch {


  def main(args: Array[String]) {

    // Pass in the input location on S3, the output location on S3, and how many partitions you want to use
    // 160 partitions works well with a 5 node cluster of x4.2xlarge instances with maximumResourceAllocation enabled

    val inputLocation = args(0)
    val outputLocation = args(1)
    var partitions = 160

    try {
      if (args(2) != null) {
        partitions = args(2).toInt
      }
    }


    val conf = new SparkConf().setAppName("Flights Example")

    // sc is the SparkContext.
    val sc = new SparkContext(conf)

    val hiveContext = new org.apache.spark.sql.hive.HiveContext(sc)
    import hiveContext.implicits._

    val parquetFile = hiveContext.read.parquet(inputLocation).repartition(partitions)


    // Persist the input since it is the same for each SQL statement
    parquetFile.persist()

    //Parquet files can also be registered as tables and then used in SQL statements.
    parquetFile.registerTempTable("flights")


    val relativeOut = singleOutputFilePath()
    //Top 10 airports with the most departures since 2000
    val topDepartures = hiveContext.sql("SELECT origin, count(*) AS total_departures FROM flights WHERE year >= '2000' GROUP BY origin ORDER BY total_departures DESC LIMIT 10")
    topDepartures.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"top_departures"))

    //Top 10 airports with the most departure delays over 15 minutes since 2000

    val shortDepDelay = hiveContext.sql("SELECT origin, count(depdelay) as cnt FROM flights WHERE depdelay >= '15' AND year >= '2000' GROUP BY origin ORDER BY cnt DESC LIMIT 10")
    shortDepDelay.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"top_short_delays"))

    //Top 10 airports with the most departure delays over 60 minutes since 2000
    val longDepDelay = hiveContext.sql("SELECT origin, count(depdelay) AS total_delays FROM flights WHERE depdelay > '60' AND year >= '2000' GROUP BY origin ORDER BY total_delays DESC LIMIT 10")
    longDepDelay.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"top_long_delays"))

    //Top 10 airports with the most departure cancellations since 2000
    val topCancel = hiveContext.sql("SELECT origin, count(cancelled) AS total_cancellations FROM flights WHERE cancelled = '1' AND year >= '2000' GROUP BY origin ORDER BY total_cancellations DESC LIMIT 10")
    topCancel.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"top_cancellations"))

    //Rank of the worst quarter of the year for departure cancellations
    val quarterCancel = hiveContext.sql("SELECT quarter, count(cancelled) AS total_cancellations FROM flights WHERE cancelled = '1' GROUP BY quarter ORDER BY total_cancellations DESC LIMIT 10")
    quarterCancel.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"rank_quarter_cancellations"))

    //Top 10 most popular flight routes since 2000
    val popularFlights = hiveContext.sql("SELECT origin, dest, count(*) AS total_flights FROM flights WHERE year >= '2000' GROUP BY origin, dest ORDER BY total_flights DESC LIMIT 10")
    popularFlights.rdd.saveAsTextFile("%s/%s/%s".format(outputLocation,relativeOut,"popular_flights"))
  }

  def singleOutputFilePath(curr:DateTime=new DateTime())={
    val s = new StringBuilder
    s.append(curr.getYear).append("/").append(pad(curr.getMonthOfYear,2)).append("/").append(pad(curr.getDayOfMonth,2)).append("/").append(curr.getMillis)
    s.toString()
  }

  def pad(valIn: Any, length:Int=2) = {
    org.apache.commons.lang.StringUtils.leftPad(valIn.toString, length, "0")
  }
}