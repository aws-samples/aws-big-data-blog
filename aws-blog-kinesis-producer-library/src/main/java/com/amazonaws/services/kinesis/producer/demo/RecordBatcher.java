package com.amazonaws.services.kinesis.producer.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;

public class RecordBatcher {
    private List<PutRecordsRequestEntry> entries = new ArrayList<>();
    private int requestSize = 0;
    
    private final int maxCount;
    private final int maxSize;
    
    public RecordBatcher() {
        this(500, 5 * 1024 * 1024);
    }
    
    public RecordBatcher(int maxCount, int maxSize) {
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }
    
    public Optional<PutRecordsRequest> put(PutRecordsRequestEntry entry) {
        int newRequestSize = requestSize + entry.getData().remaining()
                + entry.getPartitionKey().length();
        if (entries.size() < maxCount && newRequestSize <= maxSize) {
            requestSize = newRequestSize;
            entries.add(entry);
            return Optional.empty();
        } else {
            Optional<PutRecordsRequest> ret = flush();
            put(entry);
            return ret;
        }
    }
    
    public Optional<PutRecordsRequest> flush() {
        if (entries.size() > 0) {
            PutRecordsRequest r = new PutRecordsRequest();
            r.setRecords(entries);
            entries = new ArrayList<>();
            requestSize = 0;
            return Optional.of(r);
        } else {
            return Optional.empty();
        }
    }
}
