// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.blog.function;

import com.amazon.aws.blog.model.StatefulArtifactIndex;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.elasticache.model.DescribeReplicationGroupsRequest;
import com.amazonaws.services.elasticache.model.DescribeReplicationGroupsResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.spark.api.java.function.FlatMapFunction;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.DefaultJedisClientConfig;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Given a group of order IDs, retrieve the corresponding match sets from ElastiCache (if available) or from S3 using
 * the DynamoDB index entry. This is done in a multi-threaded manner to maximize throughput.
 */
public class PreFetcherFlatMapFunction implements FlatMapFunction<Iterator<String>, String> {
    private static final Integer DEFAULT_REDIRECTIONS = 5;

    private static final String S3_BUCKET_PREFIX = "stateful-artifacts-";
    private final String awsAccount;
    private final String elastiCacheID;

    public PreFetcherFlatMapFunction(final String awsAccount, final String elastiCacheID) {
        this.awsAccount = awsAccount;
        this.elastiCacheID = elastiCacheID;
    }

    /**
     * Gets the actual ElastiCache cluster endpoint given the ElastiCache cache ID. The endpoint is required when
     * creating a JedisCluster client
     * @return the cluster host and port or null if the cache does not exist
     */
    public HostAndPort getElastiCacheAddress() {
        final AmazonElastiCache elastiCache = AmazonElastiCacheClientBuilder.defaultClient();
        final DescribeReplicationGroupsRequest describeReplicationGroupsRequest = new DescribeReplicationGroupsRequest();
        describeReplicationGroupsRequest.setReplicationGroupId(elastiCacheID);
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

    @Override
    public Iterator<String> call(final Iterator<String> orderIdIterator) throws Exception {
        final AmazonDynamoDB ddbClient = AmazonDynamoDBClientBuilder.standard().build();
        final DynamoDBMapper ddbMapper = new DynamoDBMapper(ddbClient);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build();

        final Set<HostAndPort> jedisClusterNodes = new HashSet<>();
        final HostAndPort hostAndPort = getElastiCacheAddress();
        jedisClusterNodes.add(hostAndPort);
        final JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes,
                DefaultJedisClientConfig.builder().ssl(true).build(), DEFAULT_REDIRECTIONS, new GenericObjectPoolConfig<>());

        final List<String> output = new LinkedList<>();
        final List<Future<String>> futures = new LinkedList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(32);

        while (orderIdIterator.hasNext()) {
            final String orderId = orderIdIterator.next();

            // Load stateful artifacts in parallel by using multiple threads
            Callable<String> callable = () -> {
                final StatefulArtifactIndex index = ddbMapper.load(StatefulArtifactIndex.class, orderId);
                if (index == null) {
                    // There was no pre-existing stateful artifact
                    return null;
                }

                final String file = index.getFile();
                final Long offset = Long.valueOf(index.getByteOffset());
                final Long size = Long.valueOf(index.getByteSize());
                final String cachedContent = jedisCluster.get(getElastiCacheKey(file, offset, size));
                if (cachedContent == null) {
                    // Cache miss so we must default back to the backing S3 storage
                    // S3 objects can be retrieved using the partial seek feature
                    final GetObjectRequest objectRequest = new GetObjectRequest(S3_BUCKET_PREFIX + this.awsAccount, file)
                            .withRange(offset, offset + size - 1);
                    final S3Object s3Object = s3.getObject(objectRequest);

                    // Adds the retrieved stateful artifact to ElastiCache before returning
                    final String fetchedArtifact = new String(IOUtils.toByteArray(s3Object.getObjectContent()), StandardCharsets.UTF_8);
                    jedisCluster.set(getElastiCacheKey(file, offset, size), fetchedArtifact);
                    return fetchedArtifact;
                } else {
                    // Cache hit so we can return the value in ElastiCache
                    return cachedContent;
                }
            };
            futures.add(executorService.submit(callable));
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(24, TimeUnit.HOURS)) {
            return null;
        }

        for (final Future<String> future : futures) {
            final String existingMatchSet = future.get();
            if (existingMatchSet != null){
                output.add(existingMatchSet);
            }
        }

        return output.iterator();
    }

    private String getElastiCacheKey(final String key, final Long offset, final Long size) {
        return String.format("%s-%s-%s", key, offset, size);
    }
}
