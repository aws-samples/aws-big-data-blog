package com.amazonaws.services.kinesis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

/**
 * Simple factory class to generate a standalone Kinesis Aggregator
 * IRecordProcessor for the application
 */
public class ManagedClientProcessorFactory implements IRecordProcessorFactory {

	private static Map<String, Object> createdProcessors = new HashMap<>();

	private final Log LOG = LogFactory
			.getLog(ManagedClientProcessorFactory.class);

	private ManagedClientProcessor managedProcessor;

	public ManagedClientProcessorFactory(ManagedClientProcessor managedProcessor) {
		this.managedProcessor = managedProcessor;
	}

	/**
	 * {@inheritDoc}
	 */
	public IRecordProcessor createProcessor() {
		try {
			LOG.info("Creating new Managed Client Processor");
			ManagedClientProcessor p = this.managedProcessor.copy();
			createdProcessors.put(p.toString(), p);
			return p;
		} catch (Exception e) {
			LOG.error(e);
			return null;
		}
	}
}
