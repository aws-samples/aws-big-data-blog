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

public class Event {
	/**
	 * @param partitionKey The partition key
	 * @param data The byte buffer
	 */
	public Event(String partitionKey, ByteBuffer data) {
		super();
		this.partitionKey = partitionKey;
		this.data = data;
	}
	
	/**
	 * @param partitionKey The partition key
	 * @param data The byte buffer
	 */
	public Event(String partitionKey, String data) {
		super();
		this.partitionKey = partitionKey;
		this.data = ByteBuffer.wrap(data.getBytes());
	}
	

	/**
	 * The partition key to use
	 */
	private String partitionKey;
	
	/**
	 * The payload containing the data sent to Kinesis
	 */
	private ByteBuffer data;

	/**
	 * @return The partition key
	 */
	public String getPartitionKey() {
		return partitionKey;
	}

	/**
	 * @param partitionKey The partition key
	 */
	public void setPartitionKey(String partitionKey) {
		this.partitionKey = partitionKey;
	}

	/**
	 * @return The data payload
	 */
	public ByteBuffer getData() {
		return data;
	}

	/**
	 * @param data Sets the data payload
	 */
	public void setData(ByteBuffer data) {
		this.data = data;
	}
}
