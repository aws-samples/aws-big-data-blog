package com.amazonaws.proserv.blog

import com.typesafe.config.Config
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.hive.HiveContext
import spark.jobserver.{SparkHiveJob, SparkJobValid, SparkJobValidation, SparkSqlJob}
/**
  * Created by dgraeber on 5/2/2016.
  */
object FlightsSqlLoad extends SparkSqlJob  {
  override def runJob(sqlContext: SQLContext, jobConfig: Config): Any = {
    val loc = jobConfig.getString("loc")
    val parquetFile = sqlContext.read.parquet(loc).repartition(160)
    parquetFile.registerTempTable("flights")
    parquetFile.persist()

  }

  override def validate(sc: SQLContext, config: Config): SparkJobValidation = SparkJobValid
}

object FlightsSqlTest extends SparkSqlJob  {
  override def runJob(sqlContext: SQLContext, jobConfig: Config): Any = {
    sqlContext.sql(jobConfig.getString("sql")).collect()
  }
  override def validate(sc: SQLContext, config: Config): SparkJobValidation = SparkJobValid
}





object FlightsHiveLoad extends SparkHiveJob{
  override def runJob(hiveContext: HiveContext, jobConfig: Config): Any = {
    val loc = jobConfig.getString("loc")
    val parquetFile = hiveContext.read.parquet(loc).repartition(160)
    parquetFile.persist()
    parquetFile.registerTempTable("flights")

  }

  override def validate(sc: HiveContext, config: Config): SparkJobValidation = SparkJobValid
}

object FlightsHiveTest extends SparkHiveJob   {
  override def runJob(hiveContext: HiveContext, jobConfig: Config): Any = {
    hiveContext.sql(jobConfig.getString("sql")).collect()

  }
  override def validate(sc: HiveContext, config: Config): SparkJobValidation = SparkJobValid
}

