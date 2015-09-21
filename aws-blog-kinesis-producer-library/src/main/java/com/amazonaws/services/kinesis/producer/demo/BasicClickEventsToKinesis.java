package com.amazonaws.services.kinesis.producer.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

public class BasicClickEventsToKinesis extends AbstractClickEventsToKinesis {
    private final AmazonKinesis kinesis;

    public BasicClickEventsToKinesis(BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        kinesis = new AmazonKinesisClient().withRegion(
                Regions.fromName(REGION));
    }

    @Override
    protected void runOnce() throws Exception {
        ClickEvent event = inputQueue.take();
        String partitionKey = event.getSessionId();
        ByteBuffer data = ByteBuffer.wrap(
                event.getPayload().getBytes("UTF-8"));
        kinesis.putRecord(STREAM_NAME, data, partitionKey);
        recordsPut.getAndIncrement();
    }
}
