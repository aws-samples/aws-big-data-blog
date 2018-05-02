package com.example

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col

object RateCodeStatus {

  def main(args: Array[String]) {

    val conf = new SparkConf()
      .setAppName("rate-code-status")

    val session = SparkSession.builder()
      .config(conf)
      .getOrCreate()

    val bucketPath = args(0)
    val inputPath = bucketPath + "/emr-step-functions/input/tripdata.csv"
    val milesDataPath = bucketPath + "/emr-step-functions/miles-per-rate"
    val resultPath = bucketPath + "/emr-step-functions/rate-code-status"

    val taxiData = session.read
      .option("inferSchema", "true")
      .option("header", "true")
      .csv(inputPath)

    val mileRateData = session.read
      .parquet(milesDataPath)

    taxiData.createOrReplaceTempView("taxi_data")
    val result = session.sql("select RatecodeID, sum(fare_amount) from taxi_data group by RatecodeID")
      .toDF("rate_code_id", "total_fare_amount")
      .join(mileRateData, col("rate_code") === col("rate_code_id"))
      .drop("rate_code")

    result.show()
    result.coalesce(1).write.option("header", "true").csv(resultPath)
  }
}
