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


/**
*  Keys for configuration overrides (via properties file).
*/
public class ConfigKeys {
   
   /**
    * Name of the application.
    */
   public static final String APPLICATION_NAME_KEY = "appName";

   /**
    * Name of the Kinesis stream.
    */
   public static final String STREAM_NAME_KEY = "kinesisStreamName";

   /**
    * Kinesis endpoint.
    */
   public static final String KINESIS_ENDPOINT_KEY = "kinesisEndpoint";
   
   /**
    * Initial position in the stream when an application starts up for the first time.
    * Value is one of LATEST (most recent data) or TRIM_HORIZON (oldest available data).
    */
   public static final String INITIAL_POSITION_IN_STREAM_KEY = "initialPositionInStream";
   
   
   /**
    * The url of the redis cluster
    */
   public static final String REDIS_ENDPOINT = "redisEndpoint";
   
   /**
    * The port of the redis cluster
    */
   public static final String REDIS_PORT = "redisPort";
   
   private ConfigKeys() {        
   }

}