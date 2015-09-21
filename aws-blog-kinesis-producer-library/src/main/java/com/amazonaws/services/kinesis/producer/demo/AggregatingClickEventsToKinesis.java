package com.amazonaws.services.kinesis.producer.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

public class AggregatingClickEventsToKinesis
        extends AbstractClickEventsToKinesis {
    private final AmazonKinesis kinesis;

    private String partitionKey;
    private String payload;
    private int numAggregated;

    public AggregatingClickEventsToKinesis(
            BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        kinesis = new AmazonKinesisClient().withRegion(
                Regions.fromName(REGION));
        reset();
    }

    @Override
    protected void runOnce() throws Exception {
        ClickEvent event = inputQueue.take();
        String partitionKey = event.getSessionId();
        String payload = event.getPayload();

        if (this.partitionKey == null) {
            this.partitionKey = partitionKey;
        }

        if (this.payload == null) {
            this.payload = payload;
        } else {
            this.payload += "\r\n" + payload;
        }
        this.numAggregated++;

        if (this.payload.length() >= 1024) {
            kinesis.putRecord(
                    STREAM_NAME,
                    ByteBuffer.wrap(this.payload.getBytes("UTF-8")),
                    this.partitionKey);
            recordsPut.getAndAdd(numAggregated);
            reset();
        }
    }

    private void reset() {
        this.partitionKey = null;
        this.payload = null;
        this.numAggregated = 0;
    }
}
