package com.amazonaws.proserv.lambda;


import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.*;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * Created by dgraeber on 6/18/2015.
 * Updated by oliver.carr on 08/30/2017.
 */
public class KinesisToFirehose {
    private final String firehoseEndpointURL = System.getenv("FIREHOSE_ENDPOINT_URL");
    private final String firehoseSigningRegion = System.getenv("FIREHOSE_SIGNING_REGION");
    private final String deliveryStreamName = System.getenv("DELIVERY_STREAM_NAME");
    private final String deliveryStreamRoleARN = System.getenv("DELIVERY_STREAM_ROLE_ARN");
    private final String targetBucketARN = System.getenv("TARGET_BUCKET_ARN");
    private final String targetPrefix = System.getenv("TARGET_PREFIX");
    private final int intervalInSec = Integer.valueOf(System.getenv("INTERVAL_SEC"));
    private final int buffSizeInMB = Integer.valueOf(System.getenv("BUFFER_SIZE_MB"));

    private AmazonKinesisFirehose firehoseClient;
    private LambdaLogger logger;

    public void kinesisHandler(final KinesisEvent event, final Context context) {
        logger = context.getLogger();
        setup();
        event.getRecords().forEach(rec -> {
            logger.log("Got message ");
            final String msg = new String(rec.getKinesis().getData().array()) + "\n";
            final Record deliveryStreamRecord = new Record().withData(ByteBuffer.wrap(msg.getBytes()));

            final PutRecordRequest putRecordRequest = new PutRecordRequest()
                    .withDeliveryStreamName(deliveryStreamName)
                    .withRecord(deliveryStreamRecord);

            logger.log("Putting message");
            firehoseClient.putRecord(putRecordRequest);
            logger.log("Successful Put");
        });
    }

    private void setup() {
        firehoseClient = AmazonKinesisFirehoseClient.builder().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(firehoseEndpointURL, firehoseSigningRegion)).build();
        checkHoseStatus();
    }

    private void checkHoseStatus() {
        final DescribeDeliveryStreamRequest describeHoseRequest = new DescribeDeliveryStreamRequest()
                .withDeliveryStreamName(deliveryStreamName);
        DescribeDeliveryStreamResult describeHoseResult;
        String status = "UNDEFINED";
        try {
            describeHoseResult = firehoseClient.describeDeliveryStream(describeHoseRequest);
            status = describeHoseResult.getDeliveryStreamDescription().getDeliveryStreamStatus();
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
            logIt("Firehose Not Existent...will create");
            createFirehose();
            checkHoseStatus();
        }
        if (status.equalsIgnoreCase("ACTIVE")) {
            logIt("Firehose ACTIVE");
            //return;
        }
        else if (status.equalsIgnoreCase("CREATING")) {
            logIt("Firehose CREATING");
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            checkHoseStatus();
        }
        else {
            logIt("Status = " + status);
        }
    }

    private void createFirehose() {
        final BufferingHints buffHints = new BufferingHints()
                .withIntervalInSeconds(intervalInSec)
                .withSizeInMBs(buffSizeInMB);

        final ExtendedS3DestinationConfiguration s3DestConf = new ExtendedS3DestinationConfiguration()
                .withBucketARN(targetBucketARN)
                .withCompressionFormat(CompressionFormat.UNCOMPRESSED)
                .withPrefix(targetPrefix)
                .withBufferingHints(buffHints)
                .withRoleARN(deliveryStreamRoleARN);

        final CreateDeliveryStreamRequest createHoseRequest = new CreateDeliveryStreamRequest()
                .withDeliveryStreamName(deliveryStreamName)
                .withExtendedS3DestinationConfiguration(s3DestConf);

        logIt("Sending create firehose request");
        firehoseClient.createDeliveryStream(createHoseRequest);
        try {
            Thread.sleep(5000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logIt(final String message) {
        if (logger != null) {
            logger.log(message);
        }
        else {
            System.out.println(message);
        }
    }

    private void listFirehose() {
        final ListDeliveryStreamsRequest listHosesRequest = new ListDeliveryStreamsRequest();
        final ListDeliveryStreamsResult lhr = firehoseClient.listDeliveryStreams(listHosesRequest);
        lhr.getDeliveryStreamNames().forEach(this::logIt);
    }

    private void deleteFirehose() {
        deleteFirehose(deliveryStreamName);
    }

    private void deleteFirehose(final String deliveryStreamName) {
        final DeleteDeliveryStreamRequest deleteHoseRequest = new DeleteDeliveryStreamRequest();
        deleteHoseRequest.setDeliveryStreamName(deliveryStreamName);
        firehoseClient.deleteDeliveryStream(deleteHoseRequest);
    }

    private void putSampleMessages() {
        setup();
        IntStream.range(0, 20000).forEach(idx -> {
            final String message = "{\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
            final Record record = new Record()
                    .withData(ByteBuffer.wrap(message.getBytes()));
            final PutRecordRequest putRecordInHoseRequest = new PutRecordRequest()
                    .withDeliveryStreamName(deliveryStreamName)
                    .withRecord(record);

            final PutRecordResult res = firehoseClient.putRecord(putRecordInHoseRequest);
            logIt(res.toString());
        });
    }


    //FOR TESTING LOCALLY ONLY
//    public static void main(String[] args){
        //KinesisToFirehose kinesisToFirehose = new KinesisToFirehose();
        //kinesisToFirehose.checkHoseStatus();
        //kinesisToFirehose.listFirehose();
        //kinesisToFirehose.setup();
        //kinesisToFirehose.putSampleMessages();
//    }
}
