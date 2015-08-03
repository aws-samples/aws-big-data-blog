package com.amazonaws.bigdatablog.s3index;

import static com.amazonaws.bigdatablog.s3index.Util.prompt;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ObjectListing;

public class TruncateBucket {
   private static final AmazonS3 s3 = new AmazonS3Client();

   private final String bucketName;

   private final BlockingQueue<String> keys = new LinkedBlockingQueue<>();

   private volatile boolean complete = false;

   public TruncateBucket(String bucketName) {
      this.bucketName = bucketName;
   }

   public static void main(String[] args) throws IOException, InterruptedException {
      System.out.println("WARNING: This will indiscriminately delete every object in a given bucket. This operation cannot be undone.");
      final String bucketName = prompt("Bucket");
      if (!"yes".equals(prompt("Are you sure you want to delete all objects in bucket " + bucketName + "? (type 'yes' to continue)"))) {
         System.out.println("Aborting...");
         return;
      }
      final TruncateBucket truncater = new TruncateBucket(bucketName);
      truncater.truncate();

      ObjectListing results;
      do {
         results = s3.listObjects(bucketName);
         final List<KeyVersion> keys = results.getObjectSummaries().stream()
               .map((k) -> new KeyVersion(k.getKey()))
               .collect(toList());
         final DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName).withKeys(keys);
         s3.deleteObjects(deleteRequest);
      } while (results.isTruncated());

   }

   private void truncate() throws InterruptedException {
      new Thread(this::listKeys).start();
      final Collection<String> keyBuffer = new ArrayList<>(1000);
      while(true) {
         final String key = keys.poll(1, TimeUnit.SECONDS);
         if (key == null) {
            continue;
         }
         keys.add(key);
         keys.drainTo(keyBuffer, 999);
         if (keys.isEmpty() && complete) {
            return;
         } else {
            delete(keyBuffer);
         }
      }
   }

   private void delete(Collection<String> keyBuffer) {
      final String[] keyArray = keyBuffer.toArray(new String[keyBuffer.size()]);
      final DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName).withKeys(keyArray);
      s3.deleteObjects(deleteRequest);
   }

   private void listKeys() {
      ObjectListing listing = s3.listObjects(bucketName);
      addKeys(listing);
      while (listing.isTruncated()) {
         listing = s3.listNextBatchOfObjects(listing);
      }
      complete = true;
   }

   private void addKeys(ObjectListing listing) {
      keys.addAll(listing.getObjectSummaries().stream().map((o) -> o.getKey()).collect(toList()));
   }
}
