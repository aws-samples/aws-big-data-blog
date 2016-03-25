#Exploring GDELT - Geospatial Analysis using SparkR on EMR
This is the code repository for the AWS Big Data blog  - Exploring GDELT : Geospatial Intelligence using SparkR on EMR

## Prerequisites
  - Amazon Web Services account
  - Download AWS Command Line Interface (CLI) from http://aws.amazon.com/cli/ if required.

##Overview of Blog - Geospatial Analysis using SparkR on EMR
This blog demonstrates how to implement a use case for Geospatial Intelligence using SparkR on Amazon EMR. Readers are encouraged to take ideas from this blog and build their own Geospatial Application using SparkR on EMR.


##Description of GDELT project
The GDELT Project monitors the world's broadcast, print, and web news from nearly every corner of every country in over 100 languages and identifies the people, locations, organizations, counts, themes, sources, emotions, counts, quotes and events driving our global society every second of every day, creating a free open platform for computing on the entire world.
Refer to http://gdeltproject.org/#intro for more information.

##Overview of CAMEO Event Codes
CAMEO is a framework for coding event data related to news coverage. For the purpose of demonstration, we have chosen specifc events related to economy. Those specific economy related event codes are - "0211","0231","0311","0331","061","071"
Please refer to http://data.gdeltproject.org/documentation/CAMEO.Manual.1.1b3.pdf for more information on what these codes mean.

##Description of R Code Samples

##1.SparkRGeoInt.R
This code sample contains the application code for this blog. The code contains the logic for two separate use cases related to Geospatial Intelligence on the GDELT dataset. The use cases have been described later in this document.

##2.SparkRGeoIntPipes.R
This code sample provides an alternate way to implement the specified use cases using Pipeline operators, namely %>>% or Pipes. Pipeline operators are becoming increasingly popular in the R community. Magrittr provides another pipeline operator (%>%) and is similar in concept. Ultimately it is the developer's choice and depends on the use case and nature of the problem. Here it has been provided purely for illustrative purposes.

##3.SparkR_Redshift.R  -- Demonstrates Spark to Redshift Integration using SparkR
Demonstrates how to integrate SparkR with Redshift using the Spark to Redshift Connector from Databricks

##4.SparkR-Redshift.sql
Demonstrates how to integrate Spark-SQL with Redshift using the Spark to Redshift Connector from Databricks

##Description of Use cases
Use Case 1: Identifies where specific events of interest are taking place on a map(based on eventcode)
Use Case 2: Identifies Top 25 locations with highest density/frequency of events

##Example EMR cluster creation
```
aws emr create-cluster --name ”Geospatial-SparkR-Application” \
--release-label emr-4.4.0 \
--applications Name=Hive Name=Spark \
--use-default-roles \
--ec2-attributes KeyName=keypair-name \
--instance-type r3.4xlarge \
--instance-count 5 \
--no-auto-terminate \
--profile profilename
```

##link to Blog Post URL


