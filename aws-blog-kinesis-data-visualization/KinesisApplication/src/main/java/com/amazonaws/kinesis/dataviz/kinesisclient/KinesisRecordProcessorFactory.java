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

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

/**
* Used to create new record processors.
*/
public class KinesisRecordProcessorFactory implements IRecordProcessorFactory {
   
	private String redisEndpoint;
	private int redisPort;
	
   /**
    * Constructor.
    */
   public KinesisRecordProcessorFactory(String redisEndpoint, int redisPort) {
       super();
       
       this.redisEndpoint = redisEndpoint;
       this.redisPort = redisPort;
   }

   /**
    * {@inheritDoc}
    */
   public IRecordProcessor createProcessor() {
       return new KinesisRecordProcessor(redisEndpoint, redisPort);
   }

}
