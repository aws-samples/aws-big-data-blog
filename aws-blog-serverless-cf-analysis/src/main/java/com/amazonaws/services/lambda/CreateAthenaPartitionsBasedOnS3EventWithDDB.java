package com.amazonaws.services.lambda;

import com.amazonaws.athena.jdbc.shaded.com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.model.PartitionConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class CreateAthenaPartitionsBasedOnS3EventWithDDB implements RequestHandler<S3Event, Void> {

    private final PartitionConfig partitionConfig;

    public CreateAthenaPartitionsBasedOnS3EventWithDDB() {
        this(PartitionConfig.fromEnv());
    }

    CreateAthenaPartitionsBasedOnS3EventWithDDB(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    @Override
    public Void handleRequest(S3Event s3Event, Context context){

        Collection<Partition>requiredPartitions = new HashSet<>();
        TableService tableService = new TableService();
        DynamoDB dynamoDBClient=new DynamoDB(new AmazonDynamoDBClient(new EnvironmentVariableCredentialsProvider()));

        for(S3EventNotification.S3EventNotificationRecord record:s3Event.getRecords()){

            String bucket=record.getS3().getBucket().getName();
            String key=record.getS3().getObject().getKey();

            System.out.printf("S3event[Event:%s,Bucket:%s,Key:%s]%n",record.getEventName(),bucket,key);

            S3Object s3Object=new S3Object(bucket,key);

            if(s3Object.hasDateTimeKey()){
                Partition partition = partitionConfig.createPartitionFor(s3Object);

                //Check if the partition exists in DynamoDBtable, if not add the partition details to the table, skip otherwise
                if (tryAddMissingPartition(partitionConfig.dynamoDBTableName(), dynamoDBClient, partition)) {
                    requiredPartitions.add(partition);
                }
            }
        }

        if(!requiredPartitions.isEmpty()){
            tableService.addPartitions(partitionConfig.tableName(),requiredPartitions, true);
        }

        return null;
    }

    //ReturntrueifpartitionisnotinDynamoDBandaddthepartition,falseotherwise
    private boolean tryAddMissingPartition(String dyanmoDBTaableName,DynamoDB dynamoDBClient, Partition partition){

        Table ddbTable= dynamoDBClient.getTable(dyanmoDBTaableName);

        Item item=new Item()
                .withPrimaryKey("PartitionSpec",partition.spec())
                .withString("PartitionPath",partition.path())
                .withString("PartitionName", partition.name());

        PutItemSpec itemSpec=new PutItemSpec()
                .withItem(item)
                .withConditionExpression("attribute_not_exists(#ps)")
                .withNameMap(new NameMap()
                        .with("#ps","PartitionSpec"));

        try{
            ddbTable.putItem(itemSpec);
            System.out.println("Item was added to the table.PartitionSpec="+partition.spec()+"; Path="+partition.path());
            return true;
        }
        catch(ConditionalCheckFailedException e){
            System.out.println(e.toString());
            System.out.println("Item already exists. PartitionSpec="+partition.spec()+"; Path="+partition.path());
            return false;
        }
    }
}
