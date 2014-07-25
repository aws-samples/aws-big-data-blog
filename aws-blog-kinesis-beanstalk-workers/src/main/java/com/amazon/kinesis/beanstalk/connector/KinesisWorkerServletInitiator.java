package com.amazon.kinesis.beanstalk.connector;

import java.lang.reflect.Method;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KinesisWorkerServletInitiator implements ServletContextListener {
    private static final Log LOG = LogFactory.getLog(KinesisWorkerServletInitiator.class);

    private final String param = "PARAM1";

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        String consumerClassName = System.getProperty(param);
        LOG.info(String.format("Starting Kinesis Worker %s with %s", consumerClassName,
                this.getClass().getSimpleName()));

        if (consumerClassName != null && !consumerClassName.equals("")) {
            try {
                final Class consumerClass = (Class) Class.forName(consumerClassName);
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
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InstantiationException e) {
                LOG.error(e);
            }
        } else {
            LOG.warn(String.format(
                    "No Kinesis Runtime Class Configured. Environment is ready for configuration using Elastic Beanstalk Property %s",
                    param));
        }
    }
}
