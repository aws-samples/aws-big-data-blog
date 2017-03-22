package com.amazonaws.services.kinesis;

import java.net.NetworkInterface;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;

public class ManagedConsumer {
    private static final String version = ".9.0";

    private static final Log LOG = LogFactory.getLog(ManagedConsumer.class);

    private String streamName, appName, regionName, environmentName, positionInStream,
            kinesisEndpoint;

    private AWSCredentialsProvider credentialsProvider;

    private InitialPositionInStream streamPosition;

    private int failuresToTolerate = -1;

    private int maxRecords = -1;

    private KinesisClientLibConfiguration config;

    private boolean isConfigured = false;

    private ManagedClientProcessor templateProcessor;

    public ManagedConsumer(String streamName, String appName,
            ManagedClientProcessor templateProcessor) {
        this.streamName = streamName;
        this.appName = appName;
        this.templateProcessor = templateProcessor;
    }

    public int run() throws Exception {
        configure();

        System.out.println(String.format("Starting %s", appName));
        LOG.info(String.format("Running %s to process stream %s", appName, streamName));

        IRecordProcessorFactory recordProcessorFactory = new ManagedClientProcessorFactory(
                this.templateProcessor);
        Worker worker = new Worker(recordProcessorFactory, this.config);

        int exitCode = 0;
        int failures = 0;

        // run the worker, tolerating as many failures as is configured
        while (failures < failuresToTolerate || failuresToTolerate == -1) {
            try {
                worker.run();
            } catch (Throwable t) {
                LOG.error("Caught throwable while processing data.", t);

                failures++;

                if (failures < failuresToTolerate) {
                    LOG.error("Restarting...");
                }
                exitCode = 1;
            }
        }

        return exitCode;
    }

    private void assertThat(boolean condition, String message) throws Exception {
        if (!condition) {
            throw new InvalidConfigurationException(message);
        }
    }

    private void validateConfig() throws InvalidConfigurationException {
        try {
            assertThat(this.streamName != null, "Must Specify a Stream Name");
            assertThat(this.appName != null, "Must Specify an Application Name");
        } catch (Exception e) {
            throw new InvalidConfigurationException(e.getMessage());
        }
    }

    public void configure() throws Exception {
        if (!isConfigured) {
            validateConfig();

            try {
                String userAgent = "AWSKinesisManagedConsumer/" + this.version;

                if (this.positionInStream != null) {
                    streamPosition = InitialPositionInStream.valueOf(this.positionInStream);
                } else {
                    streamPosition = InitialPositionInStream.LATEST;
                }

                // append the environment name to the application name
                if (environmentName != null) {
                    appName = String.format("%s-%s", appName, environmentName);
                }

                // ensure the JVM will refresh the cached IP values of AWS
                // resources
                // (e.g. service endpoints).
                java.security.Security.setProperty("networkaddress.cache.ttl", "60");

                String workerId = NetworkInterface.getNetworkInterfaces() + ":" + UUID.randomUUID();
                LOG.info("Using Worker ID: " + workerId);

                // obtain credentials using the default provider chain or the
                // credentials provider supplied
                AWSCredentialsProvider credentialsProvider = this.credentialsProvider == null ? new DefaultAWSCredentialsProviderChain()
                        : this.credentialsProvider;

                LOG.info("Using credentials with Access Key ID: "
                        + credentialsProvider.getCredentials().getAWSAccessKeyId());

                config = new KinesisClientLibConfiguration(appName, streamName,
                        credentialsProvider, workerId).withInitialPositionInStream(streamPosition).withKinesisEndpoint(
                        kinesisEndpoint);

                config.getKinesisClientConfiguration().setUserAgent(userAgent);

                if (regionName != null) {
                    Region region = Region.getRegion(Regions.fromName(regionName));
                    config.withRegionName(region.getName());
                }

                if (this.maxRecords != -1)
                    config.withMaxRecords(maxRecords);

                if (this.positionInStream != null)
                    config.withInitialPositionInStream(InitialPositionInStream.valueOf(this.positionInStream));

                LOG.info(String.format(
                        "Amazon Kinesis Managed Client prepared for %s on %s in %s (%s) using %s Max Records",
                        config.getApplicationName(), config.getStreamName(),
                        config.getRegionName(), config.getWorkerIdentifier(),
                        config.getMaxRecords()));

                isConfigured = true;
            } catch (Exception e) {
                throw new InvalidConfigurationException(e);
            }
        }
    }

    public ManagedConsumer withKinesisEndpoint(String kinesisEndpoint) {
        this.kinesisEndpoint = kinesisEndpoint;
        return this;
    }

    public ManagedConsumer withToleratedWorkerFailures(int failuresToTolerate) {
        this.failuresToTolerate = failuresToTolerate;
        return this;
    }

    public ManagedConsumer withMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
        return this;
    }

    public ManagedConsumer withRegionName(String regionName) {
        this.regionName = regionName;
        return this;
    }

    public ManagedConsumer withEnvironment(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }

    public ManagedConsumer withCredentialsProvider(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    public ManagedConsumer withInitialPositionInStream(String positionInStream) {
        this.positionInStream = positionInStream;
        return this;
    }
}
