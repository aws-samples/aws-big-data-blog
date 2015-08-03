package com.amazonaws.bigdatablog.s3index;

import static com.amazonaws.bigdatablog.s3index.Util.SYSIN;
import static com.amazonaws.bigdatablog.s3index.Util.prompt;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.interrupted;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;
import static java.util.logging.LogManager.getLogManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.Base16;
import com.amazonaws.util.Base64;
import com.google.common.util.concurrent.RateLimiter;

public class S3DataGenerator {
   private static final Random RANDOM = new Random();
   private static final ThreadLocal<MessageDigest> DIGEST = new ThreadLocal<MessageDigest>() {
      @Override
      protected MessageDigest initialValue() {
         try {
            return MessageDigest.getInstance("MD5");
         } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
         }
      }
   };
   private static final AmazonS3 S3 = new AmazonS3Client();

   private static final int AVG_OBJ_SIZE = 50;

   private final String bucket;
   private final RateLimiter limiter;
   private final String[] serverIds;
   private final int numCustomers;

   private final Thread orchestrator = new Thread(this::generateData);
   private final ExecutorService exec = Executors.newCachedThreadPool();
   private final AtomicInteger objectCount = new AtomicInteger();
   private long startTime;

   public static void main(String[] args) throws IOException, InterruptedException {
      getLogManager().getLogger("").setLevel(WARNING);
      final String bucket = prompt("S3 bucket");
      final int numServers = parseInt(prompt("Number of servers"));
      final int objectsPerSecond = parseInt(prompt("Upload rate (objects/sec)"));

      final S3DataGenerator generator = new S3DataGenerator(bucket, objectsPerSecond, numServers);
      System.out.println("Generating objects...");
      generator.start();
      System.out.println("Hit Enter to quit");
      SYSIN.readLine();
      System.out.println("Shutting down...");
      generator.stop();
      System.out.println("Shutdown complete.");
   }

   public S3DataGenerator(String bucket, int objectsPerSecond, int numServers) {
      this.bucket = bucket;
      limiter = RateLimiter.create(objectsPerSecond);
      serverIds = generateServerIds(numServers);

      // Calculate number of customers such that each server will produce a data
      // object for approximately 10% of the customers each minute
      this.numCustomers = (int) (60. * objectsPerSecond / numServers / 0.1);
   }

   private void start() {
      startTime = currentTimeMillis();
      orchestrator.start();
   }

   private void stop() throws InterruptedException {
      orchestrator.interrupt();
      orchestrator.join();
      exec.shutdown();
      final boolean completed = exec.awaitTermination(5, SECONDS);
      if (!completed) {
         exec.shutdownNow();
      }
   }

   private void generateData() {
      long ts = currentTimeMillis();
      while (!interrupted()) {
         try {
            generateMinute(ts);
         } catch (final InterruptedException e) {
            return;
         }
         ts += 60 * 1000;
      }
   }

   private void generateMinute(final long ts) throws InterruptedException {
      for (final String serverId : serverIds) {
         generateServerData(serverId, ts);
      }
   }

   private void generateServerData(String serverId, long ts) throws InterruptedException {
      for (final int customerId : selectCustomers()) {
         limiter.acquire();
         if (interrupted()) {
            throw new InterruptedException();
         }
         exec.execute(() -> {
            generateDataFile(serverId, customerId, ts);
         });
      }
   }

   private void generateDataFile(String serverId, int customerId, long ts) {
      final byte[] content = generateContent();
      final String key = calcKey(serverId, customerId, ts);
      final boolean hasTransaction = RANDOM.nextDouble() < 0.1;
      final Map<String, String> userMetadata = singletonMap("hastransaction", Boolean.toString(hasTransaction));
      putObject(key, content, userMetadata);
      complete();
   }

   private void putObject(final String key, final byte[] content, Map<String, String> userMetadata) {
      final ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(content.length);
      metadata.setContentMD5(md5b64(content));
      metadata.setUserMetadata(userMetadata);
      final PutObjectRequest request = new PutObjectRequest(bucket, key, new
            ByteArrayInputStream(content), metadata);
      S3.putObject(request);
   }

   private void complete() {
      final int totalObjects = objectCount.incrementAndGet();
      if (totalObjects % (limiter.getRate() * 10) == 0) {
         final long duration = currentTimeMillis() - startTime;
         System.out.printf("Generated %d objects in %d seconds (%.1f objects/sec)\n", totalObjects, duration / 1000, 1000. * totalObjects / duration);
      }
   }

   private String calcKey(String serverId, int customerId, long ts) {
      final String key = String.format("%s/%tY-%<tm-%<td-%<tH-%<tM/%05d-%2$d.data", serverId, ts, customerId);
      final String prefix = md5hex(key.getBytes()).substring(0, 4);
      return prefix + "/" + key;
   }

   private byte[] generateContent() {
      int size = (int) (RANDOM.nextGaussian() * AVG_OBJ_SIZE / 3 + AVG_OBJ_SIZE);
      if (size < 1) {
         size = 1;
      }
      final byte[] content = new byte[size];
      RANDOM.nextBytes(content);
      return content;
   }

   private Collection<Integer> selectCustomers() {
      final Set<Integer> customers = new HashSet<>();
      while (customers.size() < 0.1 * numCustomers) {
         customers.add(RANDOM.nextInt(numCustomers) + 1);
      }
      return customers;
   }

   private static String[] generateServerIds(int numServers) {
      final String[] serverIds = new String[numServers];
      for (int i = 0; i < numServers; i++) {
         serverIds[i] = "i-" + toHexString(RANDOM.nextInt()).toLowerCase();
      }
      return serverIds;
   }

   private static String md5b64(byte[] content) {
      return Base64.encodeAsString(md5(content));
   }

   private static String md5hex(byte[] content) {
      return Base16.encodeAsString(md5(content));
   }

   private static byte[] md5(byte[] content) {
      final MessageDigest digest = DIGEST.get();
      digest.reset();
      final byte[] md5 = digest.digest(content);
      return md5;
   }
}
