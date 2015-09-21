package com.amazonaws.services.kinesis.producer.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;

public class KPLClickEventsToKinesis extends AbstractClickEventsToKinesis {
    private final KinesisProducer kinesis;

    public KPLClickEventsToKinesis(BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        kinesis = new KinesisProducer(new KinesisProducerConfiguration()
                .setRegion(REGION)
                .setRecordMaxBufferedTime(5000));
    }

    @Override
    protected void runOnce() throws Exception {
        ClickEvent event = inputQueue.take();
        String partitionKey = event.getSessionId();
        ByteBuffer data = ByteBuffer.wrap(
                event.getPayload().getBytes("UTF-8"));
        while (kinesis.getOutstandingRecordsCount() > 5e4) {
            Thread.sleep(1);
        }
        kinesis.addUserRecord(STREAM_NAME, partitionKey, data);
        recordsPut.getAndIncrement();
    }

    @Override
    public long recordsPut() {
        return super.recordsPut() - kinesis.getOutstandingRecordsCount();
    }

    @Override
    public void stop() {
        super.stop();
        kinesis.flushSync();
        kinesis.destroy();
    }
}
