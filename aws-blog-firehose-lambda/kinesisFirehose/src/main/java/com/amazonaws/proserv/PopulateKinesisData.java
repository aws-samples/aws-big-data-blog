package com.amazonaws.proserv;


import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by dgraeber on 7/27/2015.
 */
public class PopulateKinesisData {

    private String kinesisStream = "derektest";
    private String kinesisEndpoint = "https://kinesis.us-east-1.amazonaws.com";

    AmazonKinesisClient amazonKinesisClient;

    public static void main(String[]a){
        PopulateKinesisData pkd = new PopulateKinesisData();
        pkd.process();

    }

    private void process(){
        amazonKinesisClient = new AmazonKinesisClient(new SampleAWSCredentialProvider());
        amazonKinesisClient.setEndpoint(kinesisEndpoint);
        while(true) {
            genearateMessage();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.exit(-1);
            }
        }
    }

    private void genearateMessage(){
        PutRecordsRequest putRecordsRequest  = new PutRecordsRequest();
        putRecordsRequest.setStreamName(kinesisStream);
        List <PutRecordsRequestEntry> putRecordsRequestEntryList  = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String uniqueID = UUID.randomUUID().toString();
            String message = getUniqueMessage(uniqueID);
            PutRecordsRequestEntry putRecordsRequestEntry  = new PutRecordsRequestEntry();
            putRecordsRequestEntry.setData(ByteBuffer.wrap(message.getBytes()));
            putRecordsRequestEntry.setPartitionKey(uniqueID);
            putRecordsRequestEntryList.add(putRecordsRequestEntry);
            //System.out.println(message);
        }
        putRecordsRequest.setRecords(putRecordsRequestEntryList);
        PutRecordsResult putRecordsResult  = amazonKinesisClient.putRecords(putRecordsRequest);
        System.out.println("Put Result" + putRecordsResult);
    }


    private String getUniqueMessage(String uniqueId){

        return "{\"SomeName\":\"ImportantData\",\"SomeKey\":\"REALLY important data\",\"id\":\""+uniqueId+"\",\"ts:\":\""+new Date().getTime()+"\"}";
    }


}
