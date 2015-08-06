package com.amazonaws.services.kinesis.producer.demo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;

public class BatchedClickEventsToKinesis extends AbstractClickEventsToKinesis {
    protected AmazonKinesis kinesis;
    protected List<PutRecordsRequestEntry> entries;

    private int dataSize;

    public BatchedClickEventsToKinesis(BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        kinesis = new AmazonKinesisClient().withRegion(
                Regions.fromName(REGION));
        entries = new ArrayList<>();
        dataSize = 0;
    }

    @Override
    protected void runOnce() throws Exception {
        ClickEvent event = inputQueue.take();
        String partitionKey = event.getSessionId();
        ByteBuffer data = ByteBuffer.wrap(
                event.getPayload().getBytes("UTF-8"));
        recordsPut.getAndIncrement();

        addEntry(new PutRecordsRequestEntry()
                .withPartitionKey(partitionKey)
                .withData(data));
    }

    @Override
    public long recordsPut() {
        return super.recordsPut() - entries.size();
    }

    @Override
    public void stop() {
        super.stop();
        flush();
    }

    protected void flush() {
        kinesis.putRecords(new PutRecordsRequest()
                .withStreamName(STREAM_NAME)
                .withRecords(entries));
        entries.clear();
    }

    protected void addEntry(PutRecordsRequestEntry entry) {
        int newDataSize = dataSize + entry.getData().remaining() +
                entry.getPartitionKey().length();
        if (newDataSize <= 5 * 1024 * 1024 && entries.size() < 500) {
            dataSize = newDataSize;
            entries.add(entry);
        } else {
            flush();
            dataSize = 0;
            addEntry(entry);
        }
    }
}
