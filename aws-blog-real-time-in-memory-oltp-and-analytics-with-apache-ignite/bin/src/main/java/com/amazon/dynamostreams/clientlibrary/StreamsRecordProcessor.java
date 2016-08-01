package com.amazon.dynamostreams.clientlibrary;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import org.apache.ignite.Ignition;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.configuration.IgniteConfiguration;
import java.util.Arrays;
import org.apache.ignite.myproject.orderdata;

public class StreamsRecordProcessor implements IRecordProcessor {
	private static final Log LOG = LogFactory.getLog(StreamsRecordProcessor.class);

	private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 10;
    private String kinesisShardId;
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L;
    private long nextCheckpointTimeInMillis;

	private Integer checkpointCounter;
    private final IgniteCache<String, orderdata> cache;
    public StreamsRecordProcessor(IgniteCache<String, orderdata> cache) {
	this.cache = cache;
    }
	@Override
    public void initialize(String shardId) {
	this.kinesisShardId = shardId;
        checkpointCounter = 0;
    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
	LOG.info("Processing " + records.size() + " records from " + kinesisShardId);
    	// Process records and perform all exception handling.
        processRecordsWithRetries(records);

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(checkpointer);
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }

    private void processRecordsWithRetries(List<Record> records) {
        for(Record record : records) {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                	processSingleRecord(record);
                	processedSuccessfully = true;
                	break;
                } catch (Throwable t) {
                	System.out.println("Caught throwable while processing record " + record);
        		}

        		// backoff if we encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
                	System.out.println("Interrupted sleep" + e);
                }
            }
         if (!processedSuccessfully) {
                System.out.println("Couldn't process record " + record + ". Skipping the record.");
            }
        }
    }

    private void processSingleRecord(Record record) {

			String data = new String(record.getData().array(), Charset.forName("UTF-8"));
            if(record instanceof RecordAdapter) {
                com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) record).getInternalObject();
                
                switch(streamRecord.getEventName()) { 
                case "INSERT" : case "MODIFY" :
                    String orderId = streamRecord.getDynamodb().getNewImage().get("OrderId").getS();
		    int orderQty = Integer.parseInt(streamRecord.getDynamodb().getNewImage().get("OrderQty").getN());
		    long orderdate = Long.parseLong(streamRecord.getDynamodb().getNewImage().get("OrderDate").getN());
		    String shipmethod = streamRecord.getDynamodb().getNewImage().get("ShipMethod").getS();
		    String billaddress = streamRecord.getDynamodb().getNewImage().get("BillAddress").getS();
		    String billcity = streamRecord.getDynamodb().getNewImage().get("BillCity").getS();
		    int billpostalcode = Integer.parseInt(streamRecord.getDynamodb().getNewImage().get("BillPostalCode").getN());
		    int unitprice = Integer.parseInt(streamRecord.getDynamodb().getNewImage().get("UnitPrice").getN());
		    String productcategory = streamRecord.getDynamodb().getNewImage().get("ProductCategory").getS();
		    orderdata odata = new orderdata(orderId,orderdate,shipmethod,billaddress,billcity,billpostalcode,orderQty,unitprice,productcategory);
//		    cache.put(orderId,odata);
		    cache.addData(orderId,odata);
                    break;
                case "REMOVE" :
                    System.out.println(streamRecord.getDynamodb().getKeys().get("Id").getN());
                }
            }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
    	LOG.info("Shutting down record processor for shard: " + kinesisShardId);
        // Important to checkpoint after reaching end of shard, so we can start processing data from child shards.
        if (reason == ShutdownReason.TERMINATE) {
            checkpoint(checkpointer);
        }
    }

    private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        LOG.info("Checkpointing shard " + kinesisShardId);
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint();
                break;
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOG.info("Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                    break;
                } else {
                    LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of "
                            + NUM_RETRIES, e);
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                LOG.debug("Interrupted sleep", e);
            }
        }
    }
}
