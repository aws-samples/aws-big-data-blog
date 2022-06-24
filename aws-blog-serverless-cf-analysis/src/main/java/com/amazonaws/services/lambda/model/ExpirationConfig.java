package com.amazonaws.services.lambda.model;


import com.amazonaws.services.lambda.utils.EnvironmentVariableUtils;
import com.amazonaws.services.lambda.utils.TimeUnitParser;

import java.util.concurrent.TimeUnit;

public class ExpirationConfig {

    public static ExpirationConfig fromEnv() {
        long expiresAfterMillis = TimeUnitParser.parse(
                EnvironmentVariableUtils.getOptionalEnv("EXPIRES_AFTER", String.valueOf(TimeUnit.DAYS.toMillis(30))),
                TimeUnit.MILLISECONDS);
        return new ExpirationConfig(expiresAfterMillis);
    }

    private final long expiresAfterMillis;

    public ExpirationConfig(long expiresAfterMillis) {
        this.expiresAfterMillis = expiresAfterMillis;
    }

    public long expiresAfterMillis() {
        return expiresAfterMillis;
    }
}
