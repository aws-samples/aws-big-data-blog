package com.example

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object MilesPerRateCode {

  def main(args: Array[String]) {

    val conf = new SparkConf()
      .setAppName("miles-per-rate-code")

    val session = SparkSession.builder()
      .config(conf)
      .getOrCreate()

    val bucketPath = args(0)
    val inputPath = bucketPath + "/emr-step-functions/input/tripdata.csv"
    val outputPath = bucketPath + "/emr-step-functions/miles-per-rate"

    val taxiData = session.read
      .option("inferSchema", "true")
      .option("header", "true")
      .csv(inputPath)

    taxiData.createOrReplaceTempView("taxi_data")
    val result = session.sql("select RatecodeID as rate_code, sum(trip_distance) as total_distance from taxi_data group by RatecodeID")
    result.write.parquet(outputPath)
  }
}
