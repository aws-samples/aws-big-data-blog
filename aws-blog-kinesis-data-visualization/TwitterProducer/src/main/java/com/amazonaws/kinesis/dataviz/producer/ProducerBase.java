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

package com.amazonaws.kinesis.dataviz.producer;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;

/**
 * Runnable class responsible for sending items on the queue to Kinesis
 * @author corbetn
 *
 */
public class ProducerBase implements Runnable {

	/**
	 * Reference to the queue
	 */
	private final BlockingQueue<Event> eventsQueue;
	
	
	/**
	 * Reference to the Amazon Kinesis Client
	 */
	private final AmazonKinesis kinesisClient;
	
	
	/**
	 * The stream name that we are sending to
	 */
	private final String streamName;
	
	private final static Logger logger = LoggerFactory
			.getLogger(ProducerBase.class);


	/**
	 * @param eventsQueue The queue that holds the records to send to Kinesis
	 * @param kinesisClient Reference to the Kinesis client
	 * @param streamName The stream name to send items to
	 */
	public ProducerBase(BlockingQueue<Event> eventsQueue,
			AmazonKinesis kinesisClient, String streamName) {
		this.eventsQueue = eventsQueue;
		this.kinesisClient = kinesisClient;
		this.streamName = streamName;

	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		while (true) {
			try {
				// get message from queue - blocking so code will wait here for work to do
				Event event = eventsQueue.take();

				PutRecordRequest put = new PutRecordRequest();
				put.setStreamName(this.streamName);

				put.setData(event.getData());
				put.setPartitionKey(event.getPartitionKey());

				PutRecordResult result = kinesisClient.putRecord(put);
				logger.info(result.getSequenceNumber() + ": {}", this);	

			} catch (Exception e) {
				// didn't get record - move on to next\
				e.printStackTrace();		
			}
		}

	}
}
