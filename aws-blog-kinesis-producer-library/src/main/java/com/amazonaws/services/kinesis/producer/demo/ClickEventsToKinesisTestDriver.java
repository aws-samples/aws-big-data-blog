package com.amazonaws.services.kinesis.producer.demo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;

public class ClickEventsToKinesisTestDriver {
    private static Random RANDOM = new Random();

    private static final Duration TEST_DURATION = Duration.ofSeconds(180);

    private static ClickEvent generateClickEvent() {
        byte[] id = new byte[13];
        RANDOM.nextBytes(id);
        String data = StringUtils.repeat("a", 350);
        return new ClickEvent(DatatypeConverter.printBase64Binary(id), data);
    }

    public static void main(String[] args) throws Exception {
        final BlockingQueue<ClickEvent> events = new ArrayBlockingQueue<ClickEvent>(65536);
        final ExecutorService exec = Executors.newCachedThreadPool();

        // Change this line to use a different implementation
        final AbstractClickEventsToKinesis worker = new BasicClickEventsToKinesis(events);
        exec.submit(worker);

        // Warm up the KinesisProducer instance
        if (worker instanceof KPLClickEventsToKinesis) {
            for (int i = 0; i < 200; i++) {
                events.offer(generateClickEvent());
            }
            Thread.sleep(1000);
        }

        final LocalDateTime testStart = LocalDateTime.now();
        final LocalDateTime testEnd = testStart.plus(TEST_DURATION);

        exec.submit(() -> {
            try {
                while (LocalDateTime.now().isBefore(testEnd)) {
                    events.put(generateClickEvent());
                }

                worker.stop();
                // This will unblock worker if it's blocked on the queue
                events.offer(generateClickEvent());
                exec.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

        // This reports the average records per second over a 10 second sliding window
        new Thread(() -> {
            Map<Long, Long> history = new TreeMap<>();
            try {
                while (!exec.isTerminated()) {
                    long seconds = Duration.between(testStart, LocalDateTime.now()).getSeconds();
                    long records = worker.recordsPut();
                    history.put(seconds, records);

                    long windowStart = seconds - 10;
                    long recordsAtWinStart =
                            history.containsKey(windowStart) ? history.get(windowStart) : 0;
                    double rps = (double)(records - recordsAtWinStart) / 10;

                    System.out.println(String.format(
                            "%d seconds, %d records total, %.2f RPS (avg last 10s)",
                            seconds,
                            records,
                            rps));
                    Thread.sleep(1000);
                }
                System.out.println("Finished.");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();
    }
}
