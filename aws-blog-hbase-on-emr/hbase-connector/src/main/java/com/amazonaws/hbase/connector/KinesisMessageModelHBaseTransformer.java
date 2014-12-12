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

import java.util.HashMap;
import java.util.Map;
import com.amazonaws.hbase.kinesis.KinesisMessageModel;
import com.amazonaws.services.kinesis.connectors.BasicJsonTransformer;


/**
 * A custom transformer for {@link KinesisMessageModel} records in JSON format. 
 * The output is key/value pairs which are compatible with HBase insertions
 */
public class KinesisMessageModelHBaseTransformer extends BasicJsonTransformer<KinesisMessageModel,Map<String,String>>
        implements HBaseTransformer<KinesisMessageModel> {

    /**
     * Creates a new KinesisMessageModelHBaseTransformer.
     */
    public KinesisMessageModelHBaseTransformer() {
        super(KinesisMessageModel.class);
    }

    @Override
    public Map<String, String> fromClass(KinesisMessageModel message) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("userid", Integer.toString(message.userid));
        item.put("username", message.username);
        item.put("firstname", message.firstname);
        item.put("lastname", message.lastname);
        item.put("city", message.city);
        item.put("state", message.state);
        item.put("email", message.email);
        item.put("phone", message.phone);
        item.put("likesports", Boolean.toString(message.likesports));
        item.put("liketheatre", Boolean.toString(message.liketheatre));
        item.put("likeconcerts", Boolean.toString(message.likeconcerts));
        item.put("likejazz", Boolean.toString(message.likejazz));
        item.put("likeclassical", Boolean.toString(message.likeclassical));
        item.put("likeopera", Boolean.toString(message.likeopera));
        item.put("likerock", Boolean.toString(message.likerock));
        item.put("likevegas", Boolean.toString(message.likevegas));
        item.put("likebroadway", Boolean.toString(message.likebroadway));
        item.put("likemusicals", Boolean.toString(message.likemusicals));
        return item;
    }
}
