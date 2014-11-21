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

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

public class ProducerClient implements Producer {


	private ExecutorService executorService;
	
	private final int threads;

	private final AtomicBoolean canRun;
	
	private final String name;
	
	private final String streamName;
	
	private final AmazonKinesis kinesisClient;
	
	private final BlockingQueue<Event> eventsQueue;

	private final static Logger logger = LoggerFactory
			.getLogger(ProducerClient.class);

	/**
	 * @param name The name of the client, used for debugging purposes
	 * @param streamName The name of the stream to send data to
	 * @param threads The number of threads to put in the pool
	 * @param region The region that the kinesis stream is in
	 */
	public ProducerClient(String name, String streamName,
			int threads, Region region) {
		
		kinesisClient = new AmazonKinesisClient();
		kinesisClient.setRegion(region);
		
		eventsQueue = new LinkedBlockingQueue<Event>();

		this.name = name;
		this.canRun = new AtomicBoolean(true);
		this.threads = threads;
		this.streamName = streamName;

	}

	/**
	 * {@inheritDoc} 
	 */
	public void connect() {

		if (!canRun.compareAndSet(true, false) ) {
			throw new IllegalStateException("Already running");
		}
		
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		executorService = Executors.newFixedThreadPool(threads, threadFactory);
		
		for(int i = 0; i < this.threads; i++){
			ProducerBase p = new ProducerBase(this.eventsQueue, this.kinesisClient, this.streamName);
			executorService.execute(p);
			logger.info(name + ": New thread started : {}", p);	
		}
	
	}

	/**
	 * {@inheritDoc} 
	 */
	public void stop() {
		logger.info("Stopping the client");
		try {
			executorService.shutdownNow();

			logger.info(name + ": Successfully stopped the client");
		} catch (Exception e) {
			logger.info(
					name + ": Exception when attempting to stop the client: " + e.getMessage());
		}
	}

	/**
	 * {@inheritDoc} 
	 */
	public void post(Event event) {
		eventsQueue.offer(event);
	}
	
	/**
	 * {@inheritDoc} 
	 */
	public void post(String partitionKey, ByteBuffer data) {
		Event event = new Event(partitionKey, data);
		eventsQueue.offer(event);
	}
	
	/**
	 * {@inheritDoc} 
	 */
	public void post(String partitionKey, String data) {
		Event event = new Event(partitionKey, data);
		eventsQueue.offer(event);
	}

}
