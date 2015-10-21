package com.amazonaws.services.kinesis.beanstalk.connector;

import java.lang.reflect.Method;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.ManagedClientProcessor;
import com.amazonaws.services.kinesis.ManagedConsumer;

public class KinesisWorkerServletInitiator implements ServletContextListener {
	private static final Log LOG = LogFactory
			.getLog(KinesisWorkerServletInitiator.class);

	public final String WORKER_CLASS_NAME_PARAM = "PARAM1";
	public final String MANAGED_RECORD_PROCESSOR_CLASS_PARAM = "kinesis-irecord-processor-class";
	public final String STREAM_NAME_PARAM = "stream-name";
	public final String APP_NAME_PARAM = "application-name";
	public final String REGION_PARAM = "region";
	public final String MAX_RECORDS_PARAM = "max-records";
	public final String ENVIRONMENT_NAME_PARAM = "environment-name";
	public final String POSITION_IN_STREAM_PARAM = "initial-position-in-stream";

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		String consumerClassName = System.getProperty(WORKER_CLASS_NAME_PARAM);
		String recordProcessorClassName = System
				.getProperty(MANAGED_RECORD_PROCESSOR_CLASS_PARAM);

		if (recordProcessorClassName != null
				&& !recordProcessorClassName.equals("")) {
			String streamName = System.getProperty(STREAM_NAME_PARAM);
			String appName = System.getProperty(APP_NAME_PARAM);

			if (streamName == null || streamName.equals("") || appName == null
					|| appName.equals("")) {
				LOG.error(String
						.format("Unable to use Managed Consumer without parameters %s and %s",
								STREAM_NAME_PARAM, APP_NAME_PARAM));
			} else {
				runManagedWorker(streamName, appName, recordProcessorClassName);
			}
		} else if (consumerClassName != null && !consumerClassName.equals("")) {
			runCustomWorker(consumerClassName);
		} else {
			LOG.warn("No Kinesis Worker Class or IRecordProcessor Class Configured. Environment is ready for configuration using Elastic Beanstalk Properties");
		}
	}

	private void runManagedWorker(String streamName, String appName,
			String recordProcessorClassName) {
		LOG.info(String.format("Starting Managed Kinesis Worker using %s",
				recordProcessorClassName));

		try {
			final Class recordProcessorClass = (Class) Class
					.forName(recordProcessorClassName);

			// create a new instance of the managed client processor, based upon
			// the provided IRecordProcessor class
			ManagedClientProcessor processor = (ManagedClientProcessor) recordProcessorClass
					.newInstance();
			ManagedConsumer consumer = new ManagedConsumer(streamName, appName,
					processor);

			if (System.getProperty(REGION_PARAM) != null) {
				consumer.withRegionName(System.getProperty(REGION_PARAM));
			}

			if (System.getProperty(MAX_RECORDS_PARAM) != null) {
				consumer.withMaxRecords(Integer.parseInt(System
						.getProperty(MAX_RECORDS_PARAM)));
			}

			if (System.getProperty(ENVIRONMENT_NAME_PARAM) != null) {
				consumer.withEnvironment(System
						.getProperty(ENVIRONMENT_NAME_PARAM));
			}

			if (System.getProperty(POSITION_IN_STREAM_PARAM) != null) {
				consumer.withInitialPositionInStream(System
						.getProperty(POSITION_IN_STREAM_PARAM));
			}

			consumer.run();

		} catch (Exception e) {
			LOG.error(e);
		}
	}

	private void runCustomWorker(String consumerClassName) {
		LOG.info(String.format("Starting Kinesis Worker %s with %s",
				consumerClassName, this.getClass().getSimpleName()));

		try {
			final Class consumerClass = (Class) Class
					.forName(consumerClassName);
			final Method runMethod = consumerClass.getMethod("run", null);
			runMethod.setAccessible(true);
			final Object consumer = consumerClass.newInstance();

			final class ConsumerRunner implements Runnable {
				final Method m;

				final Object o;

				public ConsumerRunner(Method m, Object o) {
					this.m = m;
					this.o = o;
				}

				@Override
				public void run() {
					try {
						m.invoke(o, null);
					} catch (Exception e) {
						e.printStackTrace();
						LOG.error(e);
					}
				}
			}
			Thread t = new Thread(new ConsumerRunner(runMethod, consumer));
			t.start();
		} catch (ClassNotFoundException | NoSuchMethodException
				| IllegalAccessException | InstantiationException e) {
			LOG.error(e);
		}
	}
}
