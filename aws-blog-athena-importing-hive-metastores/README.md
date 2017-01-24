# Exporting External Tables from a Hive metastore and Importing into Athena

This documents 2 scripts that allows:

* Exporting external tables from a Hive metastore on AWS EMR or elsewhere, as a Hive script.
* Executing the Hive script in Athena over JDBC to import the external tables.


### Pre-requisites

* Ensure you have a working Java 1.8 runtime environment
* Install groovy if not installed
* Set the Java classpath to point to the Athena JDBC driver jar location
* Ensure you have a working Python 2.7+ environment.

The above steps on AWS EMR would be:

```
# set Java to 1.8
EMR $> export JAVA_HOME=/usr/lib/jvm/java-1.8.0

# Download Groovy and set Groovy binary in PATH
EMR $> wget https://dl.bintray.com/groovy/maven/apache-groovy-binary-2.4.7.zip
EMR $> unzip apache-groovy-binary-2.4.7.zip
EMR $> export PATH=$PATH:`pwd`/groovy-2.4.7/bin/:

# Download latest Athena JDBC driver and set it in JAVA CLASSPATH
EMR $> aws s3 cp s3://athena-downloads/drivers/AthenaJDBC41-1.0.0.jar .
EMR $> export CLASSPATH=`pwd`/AthenaJDBC41-1.0.0.jar:;
```

### Running the Scripts

#### 1. Exporting External tables from Hive metastore

The python script 'exportdatabase.py' exports external tables only from the Hive metastore saves them to a local file as a Hive script. 
```
EMR $> python exportdatabase.py <<Hive database name>> 
```
Sample output:
```
EMR $> python exportdatabase.py default

Found 10 tables in database...

Database metadata exported to default_export.hql.
```
Please note that Amazon Athena does not support every datatype and every serde supported by Hive. Please refer to the Amazon Athena documentation on supported datatypes and serdes and edit or replace contents in the generated Hive script if needed to ensure compatibility with Amazon Athena.
 
#### 2. Importing the external tables into Athena

The groovy script 'executescript.py' connects to Athena and executes the Hive script generated above. Please ensure you update the access key id, secret access key and specify a S3 bucket as 's3_staging_dir' in the script. Please also ensure that the target database specified in the command exists in Amazon Athena.

```
EMR $> groovy executescript.gvy <<target database in Athena>> <<Hive script file>>
```
Sample output:
```
$ groovy executescript.gvy playdb default_export.hql 

Found 2 statements in script...

1. Executing :DROP TABLE IF EXISTS `nyc_trips_pq`

result : OK

2. Executing :
CREATE EXTERNAL TABLE `nyc_trips_pq`(
  `vendor_name` string,
  `trip_pickup_datetime` string,
  `trip_dropoff_datetime` string,
  `passenger_count` int,
  `trip_distance` float,
  `payment_type` string,
  `are_amt` float,
  `surcharge` float,
  `mta_tax` float,
  `tip_amt` float,
  `tolls_amt` float,
  `total_amt` float)
PARTITIONED BY (
  `year` string,
  `month` string)
ROW FORMAT SERDE
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
  LOCATION
  's3://bucket/dataset2'
TBLPROPERTIES (
  'parquet.compress'='SNAPPY',
  'transient_lastDdlTime'='1478199332')

result : OK
```

Please note that the 'executescript.gvy' script can be used to execute any Hive script in Athena. e.g. you can use this script to add partitions to an existing Athena table which uses a custom partition format.

You can save the following to a file `addpartitions.hql`
```
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='01') location 's3://athena-examples/elb/raw/2015/01/01';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='02') location 's3://athena-examples/elb/raw/2015/01/02';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='03') location 's3://athena-examples/elb/raw/2015/01/03';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='04') location 's3://athena-examples/elb/raw/2015/01/04';
```
and execute the script as below:
```
EMR $> groovy executescript.gvy default addpartitions.hql 

Found 4 statements in script...

1. Executing :ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='01') location 's3://athena-examples/elb/raw/2015/01/01'

result : OK

2. Executing :
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='02') location 's3://athena-examples/elb/raw/2015/01/02'

result : OK

3. Executing :
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='03') location 's3://athena-examples/elb/raw/2015/01/03'

result : OK

4. Executing :
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='04') location 's3://athena-examples/elb/raw/2015/01/04'

result : OK

```

A sample sample_createtable.hql and sample_addpartitions.hql is included in the repo that you can use to test the 'executescript.gvy' script. You can run them as below to create the table and add partitions to it in Athena.
```
$> groovy executescript.gvy default sample_createtable.hql
$> groovy executescript default sample_addpartitions.hql
```
  
