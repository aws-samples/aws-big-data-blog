###Spark to Redshift Integration Using Databricks Connector
###
###Pre-requisites
###          1.Copy redshift jdbc jar files into following locations on EMR
###            /usr/share/aws/emr/emrfs/lib/
###            /usr/share/aws/emr/lib/
###            Note: Jar can be downloaded from link below          
### http://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection.html#download-jdbc-driver
###
###          2.Add aws keys to following file:spark-env.sh in /usr/lib/spark/conf
###            export AWS_ACCESS_KEY_ID=
###            export AWS_SECRET_ACCESS_KEY=
###
###          3.Start spark-sql pointing to databricks package
###            spark-sql --packages com.databricks:spark-redshift_2.10:0.6.0
###
###          4. Ensure Redshift port to your EMR master node IP is open
###
###For Writes into Redshift table- develop a script like the following to write to Redshift from within the SparkR context

sql(hiveContext,
"
CREATE TABLE gdeltrs
USING com.databricks.spark.redshift
OPTIONS 
(
dbtable 'gdeltrs',
tempdir 's3n://bucketname/tempfoldername/',
url 'jdbc:redshift://clustername.prefix.region.redshift.amazonaws.com:5439/dev?user=USER&password=PASSWORD'
)
AS SELECT * FROM gdelt where ActionGeo_CountryCode IN ('IN','US') AND Year >= 2014
"
);