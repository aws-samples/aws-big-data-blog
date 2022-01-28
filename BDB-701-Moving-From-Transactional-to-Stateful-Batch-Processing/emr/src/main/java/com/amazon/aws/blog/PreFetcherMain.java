// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog;

import com.amazon.aws.blog.function.PreFetcherFlatMapFunction;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Objects;

/**
 * Pre-Fetcher
 *
 * Given an input file containing new batch of order IDs, pre-fetch all existing orders with the provided order IDs.
 */
public class PreFetcherMain {

    private static final String ORDER_ID = "order_id";

    public static void main(String[] args) {
        final String awsAccount = args[0];
        final String inputName = args[1];
        final String outputName = args[2];
        final String elastiCacheEndpoint = args[3];

        // Creates a new Spark session
        SparkSession session = SparkSession.builder().appName("Demo").getOrCreate();

        // Reads input data from S3 (with headers)
        DataFrameReader dataFrameReader = session.read();
        Dataset<Row> responses = dataFrameReader.option("header", "true").csv(inputName);
        responses.printSchema();

        // Retrieves the order IDs
        JavaRDD<String> orderIds = responses.select(ORDER_ID).javaRDD().map(row -> row.getString(0));
        final JavaRDD<String> fetchedArtifacts =
                orderIds.mapPartitions(new PreFetcherFlatMapFunction(awsAccount, elastiCacheEndpoint)).filter(Objects::nonNull);

        // Write the grouped data back to S3 in one single coalesced file
        fetchedArtifacts.saveAsTextFile(outputName);

        session.stop();
    }
}
