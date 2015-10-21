package com.amazonaws.services.kinesis;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

public abstract class ManagedClientProcessor implements IRecordProcessor {
	private static final Log LOG = LogFactory
			.getLog(ManagedClientProcessor.class);

	private final int NUM_RETRIES = 10;

	private final long BACKOFF_TIME_IN_MILLIS = 100L;

	private String kinesisShardId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize(String shardId) {
		LOG.info("Initializing Managed Processor for Shard: " + shardId);
		this.kinesisShardId = shardId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract void processRecords(List<Record> records,
			IRecordProcessorCheckpointer checkpointer);

	/**
	 * The Copy method allows a subclass to implement a method which returns a
	 * new version of the template processor, which takes care of making
	 * variables threadsafe, etc
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract ManagedClientProcessor copy() throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown(IRecordProcessorCheckpointer checkpointer,
			ShutdownReason reason) {
		LOG.info("Shutting down record processor for shard: " + kinesisShardId);

		// Important to checkpoint after reaching end of shard, so we can start
		// processing data from child shards.
		if (reason == ShutdownReason.TERMINATE) {
			try {
				checkpoint(checkpointer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checkpoint with retries.
	 * 
	 * @param checkpointer
	 */
	protected void checkpoint(IRecordProcessorCheckpointer checkpointer) {
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
					LOG.error("Checkpoint failed after " + (i + 1)
							+ "attempts.", e);
					break;
				} else {
					LOG.info("Transient issue when checkpointing - attempt "
							+ (i + 1) + " of " + NUM_RETRIES, e);
				}
			} catch (InvalidStateException e) {
				// This indicates an issue with the DynamoDB table (check for
				// table, provisioned IOPS).
				LOG.error(
						"Cannot save checkpoint to the DynamoDB table used by the KinesisClientLibrary.",
						e);
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
