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

package com.amazonaws.kinesis.dataviz.kinesisclient;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
*
*/
public class KinesisRecordProcessor implements IRecordProcessor {
   
   private static final Log LOG = LogFactory.getLog(KinesisRecordProcessor.class);
   private String kinesisShardId;

   // Backoff and retry settings
   private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
   private static final int NUM_RETRIES = 10;

   // Checkpoint about once a minute
   private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L;
   private long nextCheckpointTimeInMillis;
   
   // Jackson
   private ObjectMapper mapper;
   
   // Redis
   private Jedis jedis;
   private String redisEndpoint;
   private int redisPort;
   
   private Random random;
   
   private final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
   
   /**
    * Constructor.
    */
   public KinesisRecordProcessor(String redisEndpoint, int redisPort) {
       super();
       
       this.redisEndpoint = redisEndpoint;
       this.redisPort = redisPort;
       random = new Random(System.currentTimeMillis());
   }
   
   /**
    * {@inheritDoc}
    */
   public void initialize(String shardId) {
       LOG.info("Initializing record processor for shard: " + shardId);
       this.kinesisShardId = shardId;
       
       mapper = new ObjectMapper();
       
       LOG.info("Connecting to Redis");
	   jedis = new Jedis(redisEndpoint, redisPort);
   }

   /**
    * {@inheritDoc}
    */
   public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
       LOG.info("Processing " + records.size() + " records from " + kinesisShardId);
       
       // Process records and perform all exception handling.
       processRecordsWithRetries(records);
       
       // Checkpoint once every checkpoint interval.
       if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
           checkpoint(checkpointer);
           nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
       }
       
   }

   /** Process records performing retries as needed. Skip "poison pill" records.
    * @param records
    */
   private void processRecordsWithRetries(List<Record> records) {
       for (Record record : records) {
           boolean processedSuccessfully = false;
           String data = null;
           for (int i = 0; i < NUM_RETRIES; i++) {
               try {
            	   
            	   Coordinate c = null;
            	   
            	   try {
                   // For this app, we interpret the payload as UTF-8 chars.
                   data = decoder.decode(record.getData()).toString();
                   
                   // use the ObjectMapper to read the json string and create a tree
                   JsonNode node = mapper.readTree(data);
                   
                   JsonNode geo = node.findValue("geo");
                   JsonNode coords = geo.findValue("coordinates");
                   
                   Iterator<JsonNode> elements = coords.elements();
                   
                   double lat = elements.next().asDouble();
                   double lng = elements.next().asDouble();
                   
                   c = new Coordinate(lat, lng);
                   
            	   } catch(Exception e) {
            		   // if we get here, its bad data, ignore and move on to next record
            	   }
            	   
                   if(c != null) {
                	   String jsonCoords = mapper.writeValueAsString(c);
                	   jedis.publish("loc", jsonCoords);
                   }
      
					
                   processedSuccessfully = true;
                   break;
               } catch (Throwable t) {
                   LOG.warn("Caught throwable while processing record " + record, t);
               }
               
               // backoff if we encounter an exception.
               try {
                   Thread.sleep(BACKOFF_TIME_IN_MILLIS);
               } catch (InterruptedException e) {
                   LOG.debug("Interrupted sleep", e);
               }
           }

           if (!processedSuccessfully) {
               LOG.error("Couldn't process record " + record + ". Skipping the record.");
           }
       }
   }

   /**
    * {@inheritDoc}
    */
   public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
       LOG.info("Shutting down record processor for shard: " + kinesisShardId);
       // Important to checkpoint after reaching end of shard, so we can start processing data from child shards.
       if (reason == ShutdownReason.TERMINATE) {
           checkpoint(checkpointer);
       }
   }
   


   /** Checkpoint with retries.
    * @param checkpointer
    */
   private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
       LOG.info("Checkpointing shard " + kinesisShardId);
       for (int i = 0; i < NUM_RETRIES; i++) {
           try {
               checkpointer.checkpoint();
               break;
           } catch (ShutdownException se) {
               // Ignore checkpoint if the processor instance has been shutdown (fail over).
               LOG.info("Caught shutdown exception, skipping checkpoint.", se);
               break;
           } catch (ThrottlingException e) {
               // Backoff and re-attempt checkpoint upon transient failures
               if (i >= (NUM_RETRIES - 1)) {
                   LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                   break;
               } else {
                   LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of "
                           + NUM_RETRIES, e);
               }
           } catch (InvalidStateException e) {
               // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
               LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
               break;
           }
           try {
               Thread.sleep(BACKOFF_TIME_IN_MILLIS);
           } catch (InterruptedException e) {
               LOG.debug("Interrupted sleep", e);
           }
       }
   }

}
