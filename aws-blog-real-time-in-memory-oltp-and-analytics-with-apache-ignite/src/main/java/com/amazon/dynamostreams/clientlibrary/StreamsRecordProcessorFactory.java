package com.amazon.dynamostreams.clientlibrary;

import org.apache.ignite.IgniteCache;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
	private final IgniteCache<String, OrderData> cache;

	public StreamsRecordProcessorFactory(IgniteCache<String, OrderData> cache) {
		this.cache = cache;
	}

	@Override
	public IRecordProcessor createProcessor() {
		return new StreamsRecordProcessor(cache);
	}
}
