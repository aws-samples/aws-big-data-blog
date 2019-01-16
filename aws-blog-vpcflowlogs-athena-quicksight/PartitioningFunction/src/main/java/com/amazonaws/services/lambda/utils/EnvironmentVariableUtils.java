package com.amazonaws.services.lambda.utils;

import com.amazonaws.athena.jdbc.shaded.guava.base.Strings;

public class EnvironmentVariableUtils {
    public static String getMandatoryEnv(String name) {
        if (Strings.isNullOrEmpty(System.getenv(name))) {

            throw new IllegalStateException(String.format("Missing environment variable: %s", name));
        }
        return System.getenv(name);
    }

    public static String getOptionalEnv(String name, String defaultValue){
        if (Strings.isNullOrEmpty(System.getenv(name))) {
            return defaultValue;
        }
        return System.getenv(name);
    }
}
