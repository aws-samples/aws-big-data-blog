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

package KinesisStormClickstreamApp;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.base.BaseBasicBolt;
import com.amazonaws.services.kinesis.stormspout.DefaultKinesisRecordScheme;
import com.amazonaws.util.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.services.kinesis.model.Record;

public class ParseReferrerBolt extends BaseBasicBolt {

    private static final Logger LOG = LoggerFactory.getLogger(ParseReferrerBolt.class);
    private static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    @Override
    public void execute(Tuple input,  BasicOutputCollector collector) {
        Record record = (Record)input.getValueByField(DefaultKinesisRecordScheme.FIELD_RECORD);
        ByteBuffer buffer = record.getData();
        String data = null; 
        try {
            data = decoder.decode(buffer).toString();
            JSONObject jsonObject = new JSONObject(data);

            String referrer = jsonObject.getString("referrer");

            int firstIndex = referrer.indexOf('.');
            int nextIndex = referrer.indexOf('.',firstIndex+1);
            collector.emit(new Values(referrer.substring(firstIndex+1,nextIndex)));

        } catch (CharacterCodingException|JSONException|IllegalStateException e) {
            LOG.error("Exception when decoding record ", e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("referrer"));
    }

}