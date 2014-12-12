/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Map;

import com.amazonaws.services.kinesis.connectors.interfaces.ITransformer;

/**
 * This interface defines an ITransformer that has an output type of Map of key (String)
 * to value (String) to allow records to be transformed into key/value pairs compatible with HBase.
 * 
 * @param <T>
 */
public interface HBaseTransformer<T> extends ITransformer<T, Map<String, String>> {

}
