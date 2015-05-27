package com.amazonaws.bigdatablog.indexcommoncrawl;

import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.scheme.local.TextLine;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;
import org.junit.Before;
import org.junit.Test;
import cascading.flow.FlowDef;
import cascading.flow.local.LocalFlowConnector;
import java.io.IOException;
import java.util.Properties;


public class CommonCrawlIndexTest {

    @Before
    public void doNotCareAboutOsStuff() {
        System.setProperty("line.separator", "\n");
    }

    @Test
    public void testMain() throws IOException {
        Properties properties = new ConfigReader().renderProperties(CommonCrawlIndexTest.class);
        FlowDef flowDef = CommonCrawlIndex.buildFlowDef(properties);

        if (properties.getProperty("platform").toString().compareTo("LOCAL")==0){
        //Using cascading Local connector to exclude Hadoop and just test the logic
            new LocalFlowConnector(properties).connect(flowDef).complete();
        }
        else {
                new HadoopFlowConnector(properties).connect(flowDef).complete();
        }
    }

    @Test
    public void testCreateCommonCrawlFlowDef() throws Exception {
        Properties properties = new ConfigReader().renderProperties(CommonCrawlIndexTest.class);

        String sourcePath = properties.getProperty("inPath");
        String sinkPath = properties.getProperty("testCreateCommonCrawlFlowDefOutput");
        String sinkValidationPath = properties.getProperty("testCreateCommonCrawlFlowDefOutputValidation");

        // create the Cascading "source" (input) tap to read the commonCrawl WAT file(s)
        Tap source = new FileTap(new TextLine(new Fields("line")) ,sourcePath);

        // create the Cascading "sink" (output) tap to dump the results
        Tap sink = new FileTap(new TextLine(new Fields("line")) ,sinkPath);

        //Build the Cascading Flow Definition
        FlowDef flowDef = CommonCrawlIndex.createCommonCrawlFlowDef(source, sink);
        new LocalFlowConnector(properties).connect(flowDef).complete();

        Assert.sameContent(sinkPath, sinkValidationPath);
    }
}

