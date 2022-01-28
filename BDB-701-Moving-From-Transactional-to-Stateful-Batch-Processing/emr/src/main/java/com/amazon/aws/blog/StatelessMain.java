// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog;

import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.sum;

/**
 * Stateless Processing
 *
 * Reads an input file, groups related transactions, and produces an aggregated file.
 * Triggered when a new input file is available.
 *
 * Note that normally there would be additional business logic applied (not shown).
 */
public class StatelessMain {

    private static final String ORDER_ID = "order_id";
    private static final String AMOUNT = "amount";
    private static final String VERSION = "version";

    public static void main(String[] args) {
        // Gets some input arguments (or use default if they are not available)
        final String inputName = (args.length >= 1) ? args[0] : "input.csv";
        final String outputName = (args.length >= 2) ? args[1] : "output.csv";

        // Creates a new Spark session
        SparkSession session = SparkSession.builder().appName("Demo").getOrCreate();

        // Reads input data from S3 (with headers)
        DataFrameReader dataFrameReader = session.read();
        Dataset<Row> responses = dataFrameReader.option("header", "true").csv(inputName);

        responses.printSchema();

        // Merge and aggregate data by related financial events
        Dataset<Row> aggregatedData = responses
                .groupBy(col(ORDER_ID))
                .agg(sum(AMOUNT))
                .withColumnRenamed("sum(amount)", AMOUNT)
                .withColumn(VERSION, lit(0));

        // Write the grouped data back to S3 in one single coalesced file
        aggregatedData.coalesce(1).write().option("header", "true").csv(outputName);

        session.stop();
    }
}
