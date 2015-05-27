Install:
========

1) sudo yum install git

2) git clone https://github.com/hvivani/bigdata.git

3) cd bigdata/cascading/commoncrawl.cascading.elasticsearch/

To compile in Local Mode:
=========================
modify the following values in file src/main/resources/config.properties

es.port=9200

platform=LOCAL

Compile with:
-------------

mvn clean && mvn install

To Compile in Distributed/Hadoop Mode:
======================================

modify the following values in file src/main/resources/config.properties

es.port=9202

platform=DISTRIBUTED

Compile with:
-------------

mvn clean && mvn assembly:assembly -Ddescriptor=./src/main/assembly/job.xml -e

Execute:
--------

hadoop jar /home/hadoop/bigdata/cascading/commoncrawl.cascading.elasticsearch/target/commoncrawl.cascading.elasticsearch-0.0.1-SNAPSHOT-job.jar com.amazonaws.bigdatablog.indexcommoncrawl.Main s3://aws-publicdatasets/common-crawl/crawl-data/CC-MAIN-2014-52/segments/1419447563504.69/wat/CC-MAIN-20141224185923-00099-ip-10-231-17-201.ec2.internal.warc.wat.gz
