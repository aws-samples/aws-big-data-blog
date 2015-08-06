package com.amazonaws.services.kinesis.producer.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.collect.ImmutableSet;

public class RetryingBatchedClickEventsToKinesis
        extends BatchedClickEventsToKinesis {
    private final int MAX_BACKOFF = 30000;
    private final int MAX_ATTEMPTS = 5;
    private final Set<String> RETRYABLE_ERR_CODES = ImmutableSet.of(
        "ProvisionedThroughputExceededException",
        "InternalFailure",
        "ServiceUnavailable");

    private int backoff;
    private int attempt;
    private Map<PutRecordsRequestEntry, Integer> recordAttempts;

    public RetryingBatchedClickEventsToKinesis(
            BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);
        reset();
        recordAttempts = new HashMap<>();
    }

    @Override
    protected void flush() {
        PutRecordsRequest req = new PutRecordsRequest()
                .withStreamName(STREAM_NAME)
                .withRecords(entries);

        PutRecordsResult res = null;
        try {
            res = kinesis.putRecords(req);
            reset();
        } catch (AmazonClientException e) {
            if ((e instanceof AmazonServiceException
                    && ((AmazonServiceException) e).getStatusCode() / 100 == 4)
                    || attempt == MAX_ATTEMPTS) {
                reset();
                throw e;
            }

            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            Math.min(MAX_BACKOFF, backoff *= 2);

            attempt++;
            flush();
        }

        final List<PutRecordsResultEntry> results = res.getRecords();
        List<PutRecordsRequestEntry> retries = IntStream
            .range(0, results.size())
            .mapToObj(i -> {
                PutRecordsRequestEntry e = entries.get(i);
                String errorCode = results.get(i).getErrorCode();
                int n = recordAttempts.getOrDefault(e, 1) + 1;
                // Determine whether the record should be retried
                if (errorCode != null &&
                        RETRYABLE_ERR_CODES.contains(errorCode) &&
                        n < MAX_ATTEMPTS) {
                    recordAttempts.put(e, n);
                    return Optional.of(e);
                } else {
                    recordAttempts.remove(e);
                    return Optional.<PutRecordsRequestEntry>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        entries.clear();
        retries.forEach(e -> addEntry(e));
    }

    private void reset() {
        attempt = 1;
        backoff = 100;
    }
}
