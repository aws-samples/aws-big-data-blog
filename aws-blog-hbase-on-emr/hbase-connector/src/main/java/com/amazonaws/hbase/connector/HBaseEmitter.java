/*
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.hbase.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.amazonaws.hbase.kinesis.utils.HBaseUtils;
import com.amazonaws.services.kinesis.connectors.UnmodifiableBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IEmitter;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;

/**
 * This implementation of IEmitter is used to store files from an Amazon Kinesis stream into Apache HBse. The use of
 * this class requires the configuration of an Amazon EMR cluster with Apache HBase installed. When the buffer is full, this
 * class's emit method adds the contents of the buffer to Apache HBase running on Amazon EMR. 
 */
public class HBaseEmitter implements IEmitter<Map<String,String>> {
    private static final Log LOG = LogFactory.getLog(HBaseEmitter.class);
    protected final String emrEndpoint;
    protected final String hbaseTableName;
    protected final int hbaseRestPort;
    protected final String emrPublicDns;
    protected final AmazonElasticMapReduce emrClient;
 
    public HBaseEmitter(EMRHBaseKinesisConnectorConfiguration configuration) {
    	// DynamoDB Config
        this.emrEndpoint = configuration.EMR_ENDPOINT;
        this.hbaseTableName = configuration.HBASE_TABLE_NAME;
        this.hbaseRestPort = configuration.HBASE_REST_PORT;
        this.emrPublicDns = configuration.EMR_CLUSTER_PUBLIC_DNS;
        // Client
        this.emrClient = new AmazonElasticMapReduceClient(configuration.AWS_CREDENTIALS_PROVIDER);
        this.emrClient.setEndpoint(this.emrEndpoint);
    	LOG.info("EMRHBaseEmitter.....");
    }

  
    @Override
    public List<Map<String,String>> emit(final UnmodifiableBuffer<Map<String,String>> buffer) throws IOException {
    	List<Map<String, String>> records = buffer.getRecords();
    	ListIterator<Map<String, String>> iterator = records.listIterator();
    	List<Put> batch = new ArrayList<Put>();    	
    	HashMap<String, String> hashMap = (HashMap<String, String>) iterator.next();   	
    	while (iterator.hasNext()) {
    		//start with the row key followed by column family
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("user"), Bytes.toBytes("userid"),Bytes.toBytes(hashMap.get("userid"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("user"), Bytes.toBytes("firstname"),Bytes.toBytes(hashMap.get("firstname"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("user"), Bytes.toBytes("lastname"),Bytes.toBytes(hashMap.get("lastname"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("address"), Bytes.toBytes("city"),Bytes.toBytes(hashMap.get("city"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("address"), Bytes.toBytes("state"),Bytes.toBytes(hashMap.get("state"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("contact"), Bytes.toBytes("email"),Bytes.toBytes(hashMap.get("email"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("contact"), Bytes.toBytes("phone"),Bytes.toBytes(hashMap.get("phone"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likesports"),Bytes.toBytes(hashMap.get("likesports"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("liketheatre"),Bytes.toBytes(hashMap.get("liketheatre"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likeconcerts"),Bytes.toBytes(hashMap.get("likeconcerts"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likejazz"),Bytes.toBytes(hashMap.get("likejazz"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likeclassical"),Bytes.toBytes(hashMap.get("likeclassical"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likeopera"),Bytes.toBytes(hashMap.get("likeopera"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likerock"),Bytes.toBytes(hashMap.get("likerock"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likevegas"),Bytes.toBytes(hashMap.get("likevegas"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likebroadway"),Bytes.toBytes(hashMap.get("likebroadway"))));
    		batch.add(new Put(Bytes.toBytes(hashMap.get("username")))   
    		.add(Bytes.toBytes("likes"), Bytes.toBytes("likemusicals"),Bytes.toBytes(hashMap.get("likemusicals"))));
    		
    		hashMap = (HashMap<String, String>) iterator.next();	
    	}
    	LOG.info("EMIT: " + "records ....."+batch.size());
    	HBaseUtils.addRecords(hbaseTableName, emrPublicDns, hbaseRestPort, batch);
    	return Collections.emptyList();
    	//return records;
    }

    @Override
    public void fail(List<Map<String,String>> records) {
        for (Map<String,String> record : records) {
            LOG.error("Record failed: " + record);
        }
    }

    @Override
    public void shutdown() {
    	LOG.error("Record shutting down: " );
    }

}
