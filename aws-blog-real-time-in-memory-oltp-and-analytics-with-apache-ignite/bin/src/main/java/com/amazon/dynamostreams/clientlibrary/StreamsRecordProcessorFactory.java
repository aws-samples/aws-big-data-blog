package com.amazon.dynamostreams.clientlibrary;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import org.apache.ignite.Ignition;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.myproject.orderdata;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
	private final IgniteCache<String, orderdata> cache;
	public StreamsRecordProcessorFactory(
			IgniteCache<String, orderdata> cache) {
		this.cache = cache;
	}	
	@Override
	public IRecordProcessor createProcessor() {
        return new StreamsRecordProcessor(cache);
    }
}
