// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog;

import com.amazon.aws.blog.ddb.StatefulArtifactIndex;
import com.amazon.aws.blog.model.StateIndexerCoordinatorInput;
import com.amazon.aws.blog.model.StateIndexerWorkerInput;
import com.amazon.aws.blog.model.StateIndexerWorkerOutput;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.elasticache.model.DescribeReplicationGroupsRequest;
import com.amazonaws.services.elasticache.model.DescribeReplicationGroupsResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * The State Indexer is comprised of two different types of Lambda functions.
 *
 * 1) A single Coordinator Lambda which lists part files from a stateful processing EMR step
 *    and assigns part files to worker Lambdas
 * 2) A Worker Lambda which indexes each stateful artifact in the assigned part files to DynamoDB
 */
public class StateIndexerLambdaImpl implements StateIndexer {

    private static final String STATE_INDEXER_WORKER_NAME = "STATE_INDEXER_WORKER_NAME";
    private static final Integer DEFAULT_MAX_NUMBER_OF_LAMBDA_WORKERS = 30;
    private static final Integer DEFAULT_THREADS_PER_LAMBDA_WORKER = 128;
    private static final Integer DEFAULT_REDIRECTIONS = 5;

    private static final String S3_BUCKET = "stateful-artifacts-" + System.getenv("AWS_ACCOUNT");
    private static final String ELASTICACHE_ID = System.getenv("ELASTICACHE_ID");

    private final AmazonS3 s3;
    private final StateIndexer stateIndexerInvoker;

    public StateIndexerLambdaImpl() {
        s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build();

        final String stateIndexerWorkerName = System.getenv(STATE_INDEXER_WORKER_NAME);
        final ClientConfiguration configuration = new ClientConfiguration()
                .withClientExecutionTimeout(900 * 1000)
                .withConnectionTimeout(900 * 1000)
                .withRequestTimeout(900 * 1000)
                .withSocketTimeout(900 * 1000)
                .withConnectionMaxIdleMillis(900*1000)
                .withTcpKeepAlive(true);
        final AWSLambda lambdaClient = AWSLambdaClientBuilder
                .standard()
                .withClientConfiguration(configuration)
                .build();

        stateIndexerInvoker = LambdaInvokerFactory.builder()
                .lambdaClient(lambdaClient)
                .lambdaFunctionNameResolver((method, lambdaFunction, factoryConfig) -> stateIndexerWorkerName)
                .build(StateIndexer.class);
    }

    public StateIndexerLambdaImpl(final AmazonS3 s3) {
        this.s3 = s3;
        this.stateIndexerInvoker = this;
    }

    /**
     * Runs the State Indexer Coordinator Lambda function
     *
     * Responsible for listing out all part file from a stateful processing EMR step and assigning
     * one or more files to worker Lambda functions.
     *
     * @param input the input to the State Indexer Coordinator
     * @return the (empty) output of the State Indexer Coordinator
     * @throws Exception if there is a problem running the State Indexer
     */
    public void runStateIndexerCoordinator(final StateIndexerCoordinatorInput input) throws Exception {
        final float numWorkers = DEFAULT_MAX_NUMBER_OF_LAMBDA_WORKERS;
        System.out.println("Using " + S3_BUCKET + " bucket with prefix " + input.getStatefulOutput().split("/")[input.getStatefulOutput().split("/").length - 1]);

        // Lists all part files from S3 based on the provided folder prefix
        //
        final ListObjectsRequest listRequest = new ListObjectsRequest().withBucketName(S3_BUCKET)
                .withPrefix(input.getStatefulOutput().split("/")[input.getStatefulOutput().split("/").length - 1]);
        ObjectListing listResult = s3.listObjects(listRequest);
        final List<S3ObjectSummary> summaries = listResult.getObjectSummaries();

        while (listResult.isTruncated()) {
            listResult = s3.listNextBatchOfObjects(listResult);
            summaries.addAll(listResult.getObjectSummaries());
        }

        // Retrieve the part file S3 locations, excluding any files that are not Spark part-files
        final List<String> partFiles = summaries
                .stream()
                .map(S3ObjectSummary::getKey)
                .filter(part -> part.contains("part-"))
                .collect(Collectors.toList());

        final ExecutorService executorService = Executors.newFixedThreadPool((int) numWorkers);
        List<Future<StateIndexerWorkerOutput>> futures = new LinkedList<>();

        final int totalNumParts = partFiles.size();
        final int numPartsPerWorker = (int) Math.ceil(totalNumParts / numWorkers);

        System.out.println("Using " + ELASTICACHE_ID + " cache ID");
        System.out.println("Using " + numPartsPerWorker + " parts per worker");
        System.out.println("Processing " + partFiles + " part files");

        // Assign workers in a multi-threaded fashion
        for (int i = 0; i < partFiles.size(); i += numPartsPerWorker) {
            final List<String> partsForWorker = new LinkedList<>();
            for (int j = i; j < i + numPartsPerWorker && j < partFiles.size(); j++) {
                partsForWorker.add(partFiles.get(j));
            }

            final StateIndexerWorkerInput workerInput = new StateIndexerWorkerInput(partsForWorker);
            StateIndexerWorker callable = new StateIndexerWorker(workerInput);
            futures.add(executorService.submit(callable));
        }
        executorService.shutdown();

        // Wait for all workers to complete. Consider better failure handling at this point.
        int artifactsIndexed = 0;
        for (final Future<StateIndexerWorkerOutput> future : futures) {
            try {
                final StateIndexerWorkerOutput workerOutput = future.get();
                artifactsIndexed += workerOutput.getNumArtifactsIndexed();
            } catch (InterruptedException | ExecutionException e) {
                throw new Exception("Failure while indexing", e);
            }
        }

        System.out.println("Indexed " + artifactsIndexed + " artifacts");
    }

    /**
     * Runs the State Indexer Worker Lambda function
     *
     * Responsible for indexing each assigned part file to DynamoDB.
     *
     * @param input the input to the State Indexer Worker
     * @return the output of the State Indexer Worker
     * @throws Exception if there is a problem running the State Indexer
     */
    public StateIndexerWorkerOutput runStateIndexerWorker(final StateIndexerWorkerInput input) throws Exception {

        final AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().build();
        final DynamoDBMapper ddbMapper = new DynamoDBMapper(ddbClient);

        final Set<HostAndPort> jedisClusterNodes = new HashSet<>();
        jedisClusterNodes.add(getElastiCacheAddress());
        final JedisCluster jc = new JedisCluster(jedisClusterNodes,
                DefaultJedisClientConfig.builder().ssl(true).build(), DEFAULT_REDIRECTIONS, new GenericObjectPoolConfig<>());

        final List<Map.Entry<StatefulArtifactIndex, String>> indices = new LinkedList<>();

        // Reach each part file and create the approach index entry for each stateful artifact
        //
        for (final String partFile : input.getPartFiles()) {
            GetObjectRequest objectRequest = new GetObjectRequest(S3_BUCKET, partFile);
            S3Object s3Object = s3.getObject(objectRequest);

            try (final BufferedReader reader =
                         new BufferedReader(new InputStreamReader(s3Object.getObjectContent()))) {
                long offset = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    long size = line.getBytes(StandardCharsets.UTF_8).length;
                    final String[] lineArr = line.split(",");
                    final String orderId = lineArr[0];

                    final StatefulArtifactIndex index = new StatefulArtifactIndex();
                    index.setOrderId(orderId);
                    index.setFile(partFile);
                    index.setByteSize(String.valueOf(size));
                    index.setByteOffset(String.valueOf(offset));
                    indices.add(new AbstractMap.SimpleImmutableEntry<>(index, line));

                    offset += size + 1;
                }
            } catch (IOException e) {
                throw new Exception(String.format("Could not read the part file for %s", partFile), e);
            }
        }

        // Index the part files in a multi-threaded fashion
        //
        final int numThreadsPerWorker = DEFAULT_THREADS_PER_LAMBDA_WORKER;
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreadsPerWorker);
        List<Future<Boolean>> futures = new LinkedList<>();

        for (final Map.Entry<StatefulArtifactIndex, String> indexPair : indices) {
            try {
                Callable<Boolean> callable = () -> {
                    final StatefulArtifactIndex index = indexPair.getKey();
                    ddbMapper.save(index);
                    jc.set(String.format("%s-%s-%s", index.getFile(), String.valueOf(index.getByteOffset()),
                            String.valueOf(index.getByteSize())), indexPair.getValue());
                    return true;
                };
                futures.add(executorService.submit(callable));
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception(String.format("Failure while indexing %s", indexPair.getKey().getOrderId()), e);
            }
        }
        executorService.shutdown();

        int artifactsIndexed = 0;
        for (final Future<Boolean> future : futures) {
            try {
                future.get();
                artifactsIndexed++;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new Exception("Failure while indexing", e);
            }
        }
        final StateIndexerWorkerOutput indexerWorkerOutput = new StateIndexerWorkerOutput();
        indexerWorkerOutput.setNumArtifactsIndexed(artifactsIndexed);
        return indexerWorkerOutput;
    }

    class StateIndexerWorker implements Callable<StateIndexerWorkerOutput> {
        private StateIndexerWorkerInput indexerInput;

        public StateIndexerWorker(StateIndexerWorkerInput indexerInput) {
            this.indexerInput = indexerInput;
        }

        @Override
        public StateIndexerWorkerOutput call() throws Exception {
            return stateIndexerInvoker.runStateIndexerWorker(indexerInput);
        }
    }

    /**
     * Gets the actual ElastiCache cluster endpoint given the ElastiCache cache ID. The endpoint is required when
     * creating a JedisCluster client
     * @return the cluster host and port or null if the cache does not exist
     */
    private HostAndPort getElastiCacheAddress() {
        final AmazonElastiCache elastiCache = AmazonElastiCacheClientBuilder.defaultClient();
        final DescribeReplicationGroupsRequest describeReplicationGroupsRequest = new DescribeReplicationGroupsRequest();
        describeReplicationGroupsRequest.setReplicationGroupId(ELASTICACHE_ID);
        final DescribeReplicationGroupsResult describeReplicationGroupsResult =
                elastiCache.describeReplicationGroups(describeReplicationGroupsRequest);

        System.out.println("found num groups: " + describeReplicationGroupsResult.getReplicationGroups().size());
        if (describeReplicationGroupsResult.getReplicationGroups().size() == 0) {
            return null;
        }

        // Only one replication group will be returned as we have specified the replication group ID
        final Endpoint cacheEndpoint = describeReplicationGroupsResult.getReplicationGroups().get(0)
                .getConfigurationEndpoint();
        System.out.println(describeReplicationGroupsResult.getReplicationGroups().get(0));
        System.out.println("found cache endpoint manually");
        System.out.println(cacheEndpoint);
        System.out.println(cacheEndpoint.getAddress());
        System.out.println(cacheEndpoint.getPort());
        System.out.println("------");
        return new HostAndPort(cacheEndpoint.getAddress(), cacheEndpoint.getPort());
    }
}
