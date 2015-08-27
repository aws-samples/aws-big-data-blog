package com.amazonaws.bigdatablog.indexcommoncrawl;

import cascading.property.AppProps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    public Properties renderProperties(Object caller) throws IOException {
        Properties properties = new Properties();
        String propFileName = "config.properties";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

        if (inputStream != null) {
            properties.load(inputStream);
            AppProps.setApplicationJarClass(properties, caller.getClass());
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }

        return properties;
    }
}