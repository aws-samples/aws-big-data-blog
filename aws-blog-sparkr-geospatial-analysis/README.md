#Exploring Geospatial Intelligence using SparkR on Amazon EMR
This is the code repository for the AWS Big Data blog - Exploring Geospatial Intelligence using SparkR on Amazon EMR

##Link to blog post URL


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
Readers are encouraged to utilize the code samples provided here and implement them in different ways. While the blog outlines a use case for spatial analysis of events that took place in the US, the code samples provided here represent multiple use cases for events that took place in the US as well as India. Readers can choose to perform this analysis on a different set of countries, and/or choose from other news categories of interest to them. 

##1.SparkRGeoInt.R
This code sample contains the application code for the use referenced in the blog. In addition, this sample presents another use case described later in this document. While the blog focuses on spatial analysis of data in the US only, the code samples provided here illustrate how this can be done for India. 

##2.SparkRGeoIntPipes.R
This code sample provides an alternate way to implement the use cases specified below using Pipeline operators, namely %>>% or Pipes. Pipeline operators are becoming increasingly popular in the R community. Magrittr provides another pipeline operator (%>%) and is similar in concept. Ultimately it is the reader's choice and could depend on the use case and nature of the problem. Here it has been provided purely for illustrative purposes.

##3.SparkR_Redshift.R  -- Demonstrates Spark to Redshift Integration using SparkR
Demonstrates how to integrate SparkR with Redshift using the Spark to Redshift Connector from Databricks

##4.SparkR-Redshift.sql
Demonstrates how to integrate Spark-SQL with Redshift using the Spark to Redshift Connector from Databricks

##Description of Use cases
Use Case 1: Identifies where specific events of interest are taking place on a map(based on chosen eventcode). This is the primary use case referenced in the blog.

Use Case 2: Identifies Top 25 locations in a given country with highest density/frequency of events.

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




