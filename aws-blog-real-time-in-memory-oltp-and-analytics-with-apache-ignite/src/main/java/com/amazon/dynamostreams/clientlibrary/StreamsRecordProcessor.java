package com.amazon.dynamostreams.clientlibrary;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ignite.IgniteCache;

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

public class StreamsRecordProcessor implements IRecordProcessor {
	private static final Log LOG = LogFactory.getLog(StreamsRecordProcessor.class);

	private static final long BACKOFF_TIME_IN_MILLIS = 10L;
	private static final int NUM_RETRIES = 10;
	private String kinesisShardId;

	private Integer checkpointCounter;
	private final IgniteCache<String, OrderData> cache;

	public StreamsRecordProcessor(IgniteCache<String, OrderData> cache) {
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
		try {
			processRecordsWithRetries(records);

			checkpoint(checkpointer);
		} catch (Exception e) {
			System.err.println("Unhandled Exception while processing record set. Shutdown");
		}
	}

	private void processRecordsWithRetries(List<Record> records) throws Exception {
		for (Record record : records) {
			int tryCount = 0;
			boolean processedOk = false;
			while (tryCount < NUM_RETRIES) {
				try {
					try {
						processSingleRecord(record);
						processedOk = true;
					} catch (Throwable t) {
						System.out.println("Caught throwable " + t + " while processing record " + record);
						// exponential backoff
						Thread.sleep(new Double(Math.pow(2, tryCount) * BACKOFF_TIME_IN_MILLIS).longValue());
					}
				} catch (InterruptedException e) {
					throw e;
				}
			}

			if (!processedOk) {
				throw new Exception("Unable to process record " + record.getPartitionKey() + " after " + NUM_RETRIES);
			}
		}
	}

	private void processSingleRecord(Record record) throws Exception {
		// extract the data from the record as UTF-8 encoded data
		String data = new String(record.getData().array(), Charset.forName("UTF-8"));

		// confirm that the object is a DynamoDB stream record
		if (record instanceof RecordAdapter) {
			com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) record).getInternalObject();
			String orderId;
			switch (streamRecord.getEventName()) {
			case "INSERT":
			case "MODIFY":
				orderId = streamRecord.getDynamodb().getNewImage().get("OrderId").getS();
				int orderQty = Integer.parseInt(streamRecord.getDynamodb().getNewImage().get("OrderQty").getN());
				long orderdate = Long.parseLong(streamRecord.getDynamodb().getNewImage().get("OrderDate").getN());
				String shipmethod = streamRecord.getDynamodb().getNewImage().get("ShipMethod").getS();
				String billaddress = streamRecord.getDynamodb().getNewImage().get("BillAddress").getS();
				String billcity = streamRecord.getDynamodb().getNewImage().get("BillCity").getS();
				int billpostalcode = Integer
						.parseInt(streamRecord.getDynamodb().getNewImage().get("BillPostalCode").getN());
				int unitprice = Integer.parseInt(streamRecord.getDynamodb().getNewImage().get("UnitPrice").getN());
				String productcategory = streamRecord.getDynamodb().getNewImage().get("ProductCategory").getS();

				// generate a new cache object value
				OrderData odata = new OrderData(orderId, orderdate, shipmethod, billaddress, billcity, billpostalcode,
						orderQty, unitprice, productcategory);

				cache.put(orderId, odata);
				break;
			case "REMOVE":
				orderId = streamRecord.getDynamodb().getOldImage().get("OrderId").getS();
				cache.remove(orderId);
			}
		} else {
			throw new Exception(
					String.format("Warning - record %s is not an instance of RecordAdapter", record.getPartitionKey()));
		}
	}

	@Override
	public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
		LOG.info("Shutting down record processor for shard: " + kinesisShardId);
		// Important to checkpoint after reaching end of shard, so we can start
		// processing data from child shards.
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
				// Ignore checkpoint if the processor instance has been shutdown
				// (fail over).
				LOG.info("Caught shutdown exception, skipping checkpoint.", se);
				break;
			} catch (ThrottlingException e) {
				// Backoff and re-attempt checkpoint upon transient failures
				if (i >= (NUM_RETRIES - 1)) {
					LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
					break;
				} else {
					LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of " + NUM_RETRIES, e);
				}
			} catch (InvalidStateException e) {
				// This indicates an issue with the DynamoDB table (check for
				// table, provisioned IOPS).
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
