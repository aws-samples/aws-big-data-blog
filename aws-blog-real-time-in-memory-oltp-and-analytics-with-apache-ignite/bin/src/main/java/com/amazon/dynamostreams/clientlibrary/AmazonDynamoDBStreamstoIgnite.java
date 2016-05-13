package com.amazon.dynamostreams.clientlibrary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import org.apache.ignite.Ignition;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.configuration.IgniteConfiguration;
import java.util.Arrays;
import org.apache.ignite.myproject.orderdata;
import org.apache.ignite.IgniteDataStreamer;

public class AmazonDynamoDBStreamstoIgnite {

    private static final Log LOG = LogFactory.getLog(AmazonDynamoDBStreamstoIgnite.class);
    private static Worker worker;
    private static KinesisClientLibConfiguration workerConfig;
    private static IRecordProcessorFactory recordProcessorFactory;

    private static AmazonDynamoDBStreamsAdapterClient adapterClient;
    private static AWSCredentialsProvider streamsCredentials;

    private static AmazonDynamoDBClient dynamoDBClient;
    private static AWSCredentialsProvider dynamoDBCredentials;

    private static AmazonCloudWatchClient cloudWatchClient;

    private static String streamsEndpoint = "streams.dynamodb.us-east-1.amazonaws.com";
    private static String streamArn = "arn:aws:dynamodb:us-east-1:349905090898:table/OrderDetails/stream/2016-01-16T21:59:00.129";

    private static String dynamodbEndpoint = "dynamodb.us-east-1.amazonaws.com";

//    private IgniteCache<String, orderdata> cache;
    private IgniteDataStreamer<String, orderdata> cache;
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Processing...");

        streamsCredentials = new InstanceProfileCredentialsProvider();
	dynamoDBCredentials = new InstanceProfileCredentialsProvider();
        //recordProcessorFactory = new StreamsRecordProcessorFactory();


        adapterClient = new AmazonDynamoDBStreamsAdapterClient(streamsCredentials, new ClientConfiguration());
        adapterClient.setEndpoint(streamsEndpoint);
	dynamoDBClient = new AmazonDynamoDBClient(dynamoDBCredentials, new ClientConfiguration());
	dynamoDBClient.setEndpoint(dynamodbEndpoint);

	cloudWatchClient = new AmazonCloudWatchClient(dynamoDBCredentials, new ClientConfiguration());

	TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509","54.152.85.31:47500..47509","52.91.24.72:47500..47509"));
        spi.setIpFinder(ipFinder);
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setDiscoverySpi(spi);
        cfg.setClientMode(true);
        cfg.setPeerClassLoadingEnabled(true);

        Ignite ignite = Ignition.start(cfg);
        //IgniteCache<String, orderdata> cache = Ignition.ignite().cache("dynamocache");
	IgniteDataStreamer<String, orderdata> cache = Ignition.ignite().dataStreamer("dynamocache");
        LOG.info(">>> cache acquired");

	recordProcessorFactory = new StreamsRecordProcessorFactory(cache);
        workerConfig = new KinesisClientLibConfiguration("ddbstreamsprocessing",
                streamArn, streamsCredentials, "ddbstreamsworker")
            .withMaxRecords(1000)
            .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        System.out.println("Creating worker for stream: " + streamArn);
        worker = new Worker(recordProcessorFactory, workerConfig, adapterClient, dynamoDBClient, cloudWatchClient);
        System.out.println("Starting worker...");

        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            System.err.println("Caught throwable while processing data.");
            t.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

}
