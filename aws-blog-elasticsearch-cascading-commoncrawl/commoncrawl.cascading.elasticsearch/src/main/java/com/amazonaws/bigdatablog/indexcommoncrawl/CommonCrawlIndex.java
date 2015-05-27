package com.amazonaws.bigdatablog.indexcommoncrawl;

import cascading.flow.FlowDef;
import cascading.operation.regex.RegexGenerator;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
//import cascading.scheme.local.TextLine;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;
import org.elasticsearch.hadoop.cascading.EsTap;
import java.util.Properties;

public class CommonCrawlIndex {

    public static FlowDef buildFlowDef(Properties properties){
        // create the Cascading "source" (input) tap to read the commonCrawl WAT file(s)
        Tap source=null;
        //check if we're running locally or on HDFS
        Boolean isDistributed =((properties.containsKey("platform")) && properties.getProperty("platform").toString().compareTo("DISTRIBUTED")==0);
                //(properties.getProperty("platform") == "DISTRIBUTED"));

        String inPath =  properties.getProperty("inPath");

        if (isDistributed){
                source = new Hfs(new cascading.scheme.hadoop.TextLine(new Fields("line")), inPath);
        }else {
            source = new FileTap(new cascading.scheme.local.TextLine(new Fields("line")), inPath);
        }

        // create the "sink" (output) tap that will export the data to Elasticsearch
        Tap sink = new EsTap(properties.getProperty("es.target.index"));

        //Build the Cascading Flow Definition
        return CommonCrawlIndex.createCommonCrawlFlowDef(source, sink);
    }

    public static FlowDef createCommonCrawlFlowDef(Tap source, Tap sink) {
        Pipe parsePipe = new Pipe( "exportCommonCrawlWATPipe" );

        //Add a Regular Expression to collect the envelope json field from each line in the file
        RegexGenerator splitter=new RegexGenerator(new Fields("json"),"^\\{\"Envelope\".*$");
        parsePipe = new Each( parsePipe, new Fields( "line" ), splitter, Fields.RESULTS );

        // connect the taps, pipes, etc., into a flow
        return FlowDef.flowDef()
                .addSource( parsePipe, source )
                .addTailSink( parsePipe, sink );
    }





}

