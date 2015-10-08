package com.amazonaws.proserv.lambda;

import com.amazonaws.proserv.SampleAWSCredentialProvider;
import com.amazonaws.services.firehose.AmazonFirehoseClient;
import com.amazonaws.services.firehose.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

/**
 * Created by dgraeber on 6/18/2015.
 */
public class KinesisToFirehose {
    private String firehoseName = "blogfirehose";
    private String firehoseEndpointURL = "https://firehose.us-east-1.amazonaws.com";
    private String firehoseIAMRole = "firehoseRole";
    private String targetBucketName = "dgraeberaws-testing";
    private String targetPrefix = "blogoutput";
    private int intervalInSec = 60;
    private int buffSizeInMB = 2;

    private AmazonFirehoseClient firehoseClient = new AmazonFirehoseClient();
    private LambdaLogger logger;

    public void kinesisHandler(KinesisEvent event, Context context){
        logger = context.getLogger();
        setup();
        for(KinesisEvent.KinesisEventRecord rec : event.getRecords()) {
            logger.log("Got message ");
            String msg = new String(rec.getKinesis().getData().array())+"\n";
            Record record = new Record()
                    .withData(ByteBuffer.wrap(msg.getBytes()));

            PutRecordRequest putRecordRequest = new PutRecordRequest()
                    .withFirehoseName(firehoseName)
                    .withRecord(record);

            logger.log("Putting message");
            firehoseClient.putRecord(putRecordRequest);
            logger.log("Successful Put");
        }
    }

    private void setup(){
        firehoseClient = new AmazonFirehoseClient();
        firehoseClient.setEndpoint(firehoseEndpointURL);
        checkHoseStatus();
    }

    private void checkHoseStatus(){
        DescribeFirehoseRequest describeHoseRequest = new DescribeFirehoseRequest()
                .withFirehoseName(firehoseName);
        DescribeFirehoseResult  describeHoseResult = null;
        String status = "UNDEFINED";
        try {
            describeHoseResult = firehoseClient.describeFirehose(describeHoseRequest);
            status = describeHoseResult.getFirehoseDescription().getFirehoseStatus();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            logIt("Firehose Not Existent...will create");
            createFirehose();
            checkHoseStatus();
        }
        if(status.equalsIgnoreCase("ACTIVE")){
            logIt("Firehose ACTIVE");
            //return;
        }
        else if(status.equalsIgnoreCase("CREATING")){
            logIt("Firehose CREATING");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            checkHoseStatus();
        }
        else {
            logIt("Status = " + status);
        }
    }

    private void createFirehose(){
        BufferingHints buffHints = new BufferingHints()
                .withIntervalInSeconds(intervalInSec)
                .withSizeInMBs(buffSizeInMB);

        S3DestinationConfiguration s3DestConf = new S3DestinationConfiguration()
                .withBucketName(targetBucketName)
                .withCompressionFormat(CompressionFormat.UNCOMPRESSED)
                .withPrefix(targetPrefix)
                .withBufferingHints(buffHints);

        CreateFirehoseRequest createHoseRequest = new  CreateFirehoseRequest()
                .withFirehoseName(firehoseName)
                .withS3DestinationConfiguration(s3DestConf);

        createHoseRequest.setRole(firehoseIAMRole);

        logIt("Sending create firehose request");
        firehoseClient.createFirehose(createHoseRequest);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void logIt(String message){
        if(logger!=null)
            logger.log(message);
        else
            System.out.println(message);
    }

    private void listFirehose(){
        ListFirehosesRequest listHosesRequest = new ListFirehosesRequest();
        ListFirehosesResult lhr = firehoseClient.listFirehoses(listHosesRequest);

        for(String name:lhr.getFirehoseNames()){
            logIt(name);
        }
    }


    private void deleteFirehose(){
        DeleteFirehoseRequest deleteHoseRequest= new DeleteFirehoseRequest();
        deleteHoseRequest.setFirehoseName(firehoseName);
        firehoseClient.deleteFirehose(deleteHoseRequest);
    }


    private void putSampleMessages(){
        setup();
        for(int i = 0; i<20000; i++) {
            String message = "{\"timestamp\":\"" + new Date().getTime() + "\"}";
            Record record = new Record()
                    .withData(ByteBuffer.wrap(message.getBytes()));
            PutRecordRequest putRecordInHoseRequest = new PutRecordRequest()
                    .withFirehoseName(firehoseName)
                    .withRecord(record);

            PutRecordResult res = firehoseClient.putRecord(putRecordInHoseRequest);
            logIt(res.toString());
        }
    }


//    public static void main(String[] args){
//        KinesisToFirehose kinesisToFirehose = new KinesisToFirehose();
//
//        //kinesisToFirehose.checkHoseStatus();
//        //kinesisToFirehose.listFirehose();
//        //kinesisToFirehose.setup();
//        kinesisToFirehose.putSampleMessages();
//    }





}
