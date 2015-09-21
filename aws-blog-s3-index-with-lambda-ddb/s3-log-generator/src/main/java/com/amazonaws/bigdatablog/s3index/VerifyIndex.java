package com.amazonaws.bigdatablog.s3index;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class VerifyIndex {
   private static final AmazonS3 S3 = new AmazonS3Client();
   private static final AmazonDynamoDB DDB = new AmazonDynamoDBClient();

   private final SortedSet<String> bucketKeys = new TreeSet<>();
   private final SortedSet<String> indexKeys = new TreeSet<>();

   public static void main(String[] args) {
      final VerifyIndex verify = new VerifyIndex();
      verify.verify();
   }

   private void verify() {
      fetchObjectList();
      fetchIndexList();
      final String[] bucketArr = bucketKeys.toArray(new String[bucketKeys.size()]);
      final String[] indexArr = indexKeys.toArray(new String[indexKeys.size()]);

      int i = 0;
      int j = 0;
      while (i < bucketArr.length && j < indexArr.length) {
         final String bucketKey = bucketArr[i];
         final String indexKey = indexArr[j];

         final int cmp = bucketKey.compareTo(indexKey);
         if (cmp == 0) {
            i++;
            j++;
         } else if (cmp > 0) {
            System.err.println("Index contains key " + indexKey + " which is missing from bucket");
            j++;
         } else {
            System.err.println("Bucket contains key " + bucketKey + " which is missing from index");
            i++;
         }
      }
      for (; i < bucketArr.length; i++) {
         System.err.println("Bucket contains key " + bucketArr[i] + " which is missing from index");
      }
      for (; j < indexArr.length; j++) {
         System.err.println("Index contains key " + indexArr[j] + " which is missing from bucket");
      }
   }

   private void fetchIndexList() {
      final ScanRequest req = new ScanRequest("s3-index-example.mikedeck-index").withAttributesToGet("Key");
      ScanResult result = DDB.scan(req);
      addIndexItems(result.getItems());
      while (result.getLastEvaluatedKey() != null && !result.getLastEvaluatedKey().isEmpty()) {
         req.setExclusiveStartKey(result.getLastEvaluatedKey());
         result = DDB.scan(req);
         addIndexItems(result.getItems());
      }
   }

   private void addIndexItems(List<Map<String, AttributeValue>> items) {
      items.stream().forEach(i -> indexKeys.add(i.get("Key").getS()));
   }

   private void fetchObjectList() {
      ObjectListing objects = S3.listObjects("s3-index-example.mikedeck");
      addSummaries(objects.getObjectSummaries());
      while (objects.isTruncated()) {
         objects = S3.listNextBatchOfObjects(objects);
         addSummaries(objects.getObjectSummaries());
      }
   }

   private void addSummaries(final List<S3ObjectSummary> summaries) {
      for (final S3ObjectSummary summary : summaries) {
         bucketKeys.add(summary.getKey());
      }
   }
}
