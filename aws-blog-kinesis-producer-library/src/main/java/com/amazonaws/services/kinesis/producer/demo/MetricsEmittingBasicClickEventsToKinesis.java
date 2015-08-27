package com.amazonaws.services.kinesis.producer.demo;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.PutRecordResult;

public class MetricsEmittingBasicClickEventsToKinesis
        extends AbstractClickEventsToKinesis {
    private final AmazonKinesis kinesis;
    private final AmazonCloudWatch cw;

    protected MetricsEmittingBasicClickEventsToKinesis(
            BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        kinesis = new AmazonKinesisClient().withRegion(
                Regions.fromName(REGION));
        cw = new AmazonCloudWatchClient().withRegion(Regions.fromName(REGION));
    }

    @Override
    protected void runOnce() throws Exception {
        ClickEvent event = inputQueue.take();
        String partitionKey = event.getSessionId();
        ByteBuffer data = ByteBuffer.wrap(
                event.getPayload().getBytes("UTF-8"));
        recordsPut.getAndIncrement();

        PutRecordResult res = kinesis.putRecord(
                STREAM_NAME, data, partitionKey);

        MetricDatum d = new MetricDatum()
            .withDimensions(
                new Dimension().withName("StreamName").withValue(STREAM_NAME),
                new Dimension().withName("ShardId").withValue(res.getShardId()),
                new Dimension().withName("Host").withValue(
                        InetAddress.getLocalHost().toString()))
            .withValue(1.0)
            .withMetricName("RecordsPut");
        cw.putMetricData(new PutMetricDataRequest()
            .withMetricData(d)
            .withNamespace("MySampleProducer"));
    }
}
