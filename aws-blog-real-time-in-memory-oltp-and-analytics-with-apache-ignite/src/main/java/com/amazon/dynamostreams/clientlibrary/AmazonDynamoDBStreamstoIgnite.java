package com.amazon.dynamostreams.clientlibrary;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;

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

	private static String streamsEndpoint = Properties.getString("streamsEndpoint");
	private static String streamArn = Properties.getString("streamArn");

	private static String dynamodbEndpoint = Properties.getString("dynamodbEndpoint");

	private IgniteCache<String, OrderData> cache;

	public void run() throws Exception {
		adapterClient = new AmazonDynamoDBStreamsAdapterClient(new ClientConfiguration());
		adapterClient.setEndpoint(streamsEndpoint);
		dynamoDBClient = new AmazonDynamoDBClient(new ClientConfiguration());
		dynamoDBClient.setEndpoint(dynamodbEndpoint);

		cloudWatchClient = new AmazonCloudWatchClient(dynamoDBCredentials, new ClientConfiguration());

		TcpDiscoverySpi spi = new TcpDiscoverySpi();
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		List<String> hostList = Arrays.asList(Properties.getString("hostList").split(","));
		ipFinder.setAddresses(hostList);
		spi.setIpFinder(ipFinder);
		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setDiscoverySpi(spi);
		cfg.setClientMode(true);
		cfg.setPeerClassLoadingEnabled(true);

		@SuppressWarnings("unused")
		Ignite ignite = Ignition.start(cfg);
		cache = Ignition.ignite().cache(Properties.getString("cacheName"));
		LOG.info(">>> cache acquired");

		recordProcessorFactory = new StreamsRecordProcessorFactory(cache);
		workerConfig = new KinesisClientLibConfiguration(Properties.getString("applicationName"), streamArn,
				streamsCredentials, "ddbstreamsworker")
						.withMaxRecords(Integer.parseInt(Properties.getString("maxRecords")))
						.withInitialPositionInStream(
								InitialPositionInStream.valueOf(Properties.getString("initialPositionInStream")));

		LOG.info("Creating worker for stream: " + streamArn);
		worker = new Worker(recordProcessorFactory, workerConfig, adapterClient, dynamoDBClient, cloudWatchClient);
		LOG.info("Starting worker...");

		int exitCode = 0;
		try {
			worker.run();
		} catch (Throwable t) {
			LOG.error("Caught throwable while processing data.");
			t.printStackTrace();
			exitCode = 1;
		}
		System.exit(exitCode);
	}

	public static void main(String[] args) throws Exception {
		LOG.info("Starting Processing...");

		new AmazonDynamoDBStreamstoIgnite().run();
	}
}
