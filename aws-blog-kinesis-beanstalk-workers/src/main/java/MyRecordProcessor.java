import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.ManagedClientProcessor;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.KinesisClientLibDependencyException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

public class MyRecordProcessor extends ManagedClientProcessor {
	private final Log LOG = LogFactory.getLog(MyRecordProcessor.class);

	@Override
	public void processRecords(List<Record> records,
			IRecordProcessorCheckpointer checkpointer) {
		LOG.info(String.format("Received %s Records", records.size()));
		
		// add a call to your business logic here!
		//
		// myLinkedClasses.doSomething(records)
		//
		//
		try {
			checkpointer.checkpoint();
		} catch (KinesisClientLibDependencyException | InvalidStateException
				| ThrottlingException | ShutdownException e) {
			e.printStackTrace();
			super.shutdown(checkpointer, ShutdownReason.ZOMBIE);
		}
	}

	@Override
	public ManagedClientProcessor copy() throws Exception {
		return new MyRecordProcessor();
	}
}
