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

/**
 * Interface for sending information to Amazon Kinesis.
 *
 */
public interface Producer {
	/**
	 * Posts an event to Kinesis
	 * @param partitionKey The partition key to use
	 * @param data The event data, represented as a string
	 */
	void post(String partitionKey, String data);
	
	/**
	 * Posts an event to Kinesis
	 * @param partitionKey The partition key to use
	 * @param data The event data, represented as a byte buffer
	 */
	void post(String partitionKey, ByteBuffer data);
	
	/**
	 * Posts an event to Kinesis
	 * @param event The event data
	 */
	void post(Event event);
	
	
	/**
	 * Connects to Kinesis and starts listening for events to send
	 */
	void connect();
	
	/**
	 * Stops listening for events
	 */
	void stop();
	
}
