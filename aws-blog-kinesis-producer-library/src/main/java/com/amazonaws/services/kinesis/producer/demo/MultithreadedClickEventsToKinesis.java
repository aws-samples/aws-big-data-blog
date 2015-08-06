package com.amazonaws.services.kinesis.producer.demo;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultithreadedClickEventsToKinesis
        extends AbstractClickEventsToKinesis {
    private final List<BasicClickEventsToKinesis> children;
    private final ExecutorService executor;

    public MultithreadedClickEventsToKinesis(
            BlockingQueue<ClickEvent> inputQueue) {
        super(inputQueue);

        children = IntStream.range(0, 70)
                .mapToObj(i -> new BasicClickEventsToKinesis(inputQueue))
                .collect(Collectors.toList());

        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        children.forEach(c -> executor.submit(c));
    }

    @Override
    protected void runOnce() throws Exception {
        // never called
    }

    @Override
    public long recordsPut() {
       return children.stream().mapToLong(
               BasicClickEventsToKinesis::recordsPut).sum();
    }

    @Override
    public void stop() {
        children.forEach(BasicClickEventsToKinesis::stop);
    }
}
