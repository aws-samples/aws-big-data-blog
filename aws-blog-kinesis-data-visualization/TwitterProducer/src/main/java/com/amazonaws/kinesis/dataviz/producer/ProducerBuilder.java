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

import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class ProducerBuilder {

	private static final AtomicInteger clientNum = new AtomicInteger(0);
	private String streamName;
	private String name;
	private Region region;
	private int threads = 1;

	public ProducerBuilder() {
		name = "producer-client" + clientNum.getAndIncrement();
	}

	/**
	 * @param name
	 *            Name of the client used for logging and other diagnostic
	 *            purposes.
	 */
	public ProducerBuilder withName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param streamName
	 *            Name of the Kinesis stream
	 * @return
	 */
	public ProducerBuilder withStreamName(String streamName) {
		this.streamName = streamName;
		return this;
	}

	public ProducerBuilder withRegion(String regionName) {
		Region region = Region.getRegion(Regions.fromName(regionName));
		this.region = region;
		return this;
	}

	public ProducerBuilder withThreads(int threads) {
		this.threads = threads;
		return this;
	}

	/**
	 * Builds the producer client
	 * 
	 * @return A new producer client
	 */
	public ProducerClient build() {

		return new ProducerClient(name, streamName, threads, region);
	}
}
