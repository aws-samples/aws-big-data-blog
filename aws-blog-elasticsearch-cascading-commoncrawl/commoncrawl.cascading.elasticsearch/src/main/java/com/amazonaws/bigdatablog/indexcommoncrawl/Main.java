package com.amazonaws.bigdatablog.indexcommoncrawl;

import cascading.flow.FlowDef;
import cascading.flow.hadoop.HadoopFlowConnector;
import java.io.IOException;
import java.util.Properties;
import static com.amazonaws.bigdatablog.indexcommoncrawl.CommonCrawlIndex.buildFlowDef;

public class Main {
    public static void main(String args[]) {
        Properties properties = null;
        try {
            properties = new ConfigReader().renderProperties(Main.class);
            if (args[0] != null && args[0].length() > 0){
                properties.put("inPath", args[0]);
            }
        } catch (IOException e) {
            System.out.println("Could not read your config.properties file");e.printStackTrace();
        }

        FlowDef flowDef = buildFlowDef(properties);
        new HadoopFlowConnector(properties).connect(flowDef).complete();
    }

}
