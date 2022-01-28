// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;

import java.util.LinkedList;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.desc;
import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.row_number;
import static org.apache.spark.sql.functions.sum;

/**
 * Stateful Processing
 *
 * Joins the stateless artifacts produced by the Stateless Processor with existing match sets from the Pre-Fetcher and
 * any unindexed match sets from previous runs of the Stateful Processor. This produces a file containing updated
 * states of all match sets which gets saved back to S3.
 */
public class StatefulMain {

    private static final String ORDER_ID = "order_id";
    private static final String AMOUNT = "amount";
    private static final String VERSION = "version";

    public static void main(String[] args) {
        StatefulMain statefulMain = new StatefulMain();
        statefulMain.run(args);
    }

    public void run(String[] args) {
        final String statelessFile = args[0];
        final String preFetchedFile = args[1];
        final String outputName = args[2];
        final String inflightFiles;
        if (args.length > 3) {
            inflightFiles = args[3];
        } else {
            inflightFiles = null;
        }

        System.out.println("Run in beginning");

        // Creates a new Spark session
        final SparkSession session = SparkSession.builder().appName("Demo").getOrCreate();

        DataFrameReader dataFrameReader = session.read();

        System.out.println("Before reading");

        // Stateless artifacts produced by the Stateless Ingestion Engine
        Dataset<Row> statelessRows = dataFrameReader.option("header", "true").csv(statelessFile);
        // Existing stateful artifacts produced by the Pre-Fetcher
        Dataset<Row> preFetchedRows = dataFrameReader.option("header", "false").csv(preFetchedFile);
        if (preFetchedRows.count() > 0) {
            preFetchedRows = preFetchedRows.toDF(ORDER_ID, AMOUNT, VERSION);
        } else {
            // Sometimes there aren't any existing stateful artifacts, so we use a placeholder empty data frame
            preFetchedRows = session.createDataFrame(new LinkedList<>(), statelessRows.schema());
        }

        Dataset<Row> existingRows;
        if (inflightFiles != null) {
            // In-flight files contain stateful artifacts that have been indexed yet
            Dataset<Row> inflightRows = dataFrameReader.option("header", "false").csv(inflightFiles)
                    .toDF(ORDER_ID, AMOUNT, VERSION);
            existingRows = preFetchedRows.union(inflightRows);
        } else {
            existingRows = preFetchedRows;
        }

        // Keep only the rows which have the latest version between the pre-fetched rows and the unindexed rows
        Dataset<Row> latestVersions = existingRows.withColumn("temp_col", row_number().over(Window.partitionBy(ORDER_ID).orderBy(desc(VERSION))));
        latestVersions = latestVersions.where(latestVersions.col("temp_col").equalTo(1)).drop(latestVersions.col("temp_col"));

        System.out.println("Latest versions:");
        System.out.println(latestVersions.toString());

        // Update the existing rows with the new stateless artifacts
        latestVersions = latestVersions.union(statelessRows)
                .groupBy(col(ORDER_ID))
                .agg(sum(AMOUNT), max(VERSION))
                .withColumnRenamed("sum(amount)", AMOUNT)
                .withColumnRenamed("max(version)", VERSION)
                .withColumn(VERSION, expr(VERSION + " +1").cast("integer"));

        // Write the grouped data back to S3 in one single coalesced file
        latestVersions.coalesce(1).write().option("header", "false").csv(outputName);

        session.stop();
    }
}
