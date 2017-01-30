DROP TABLE IF EXISTS cars;

CREATE EXTERNAL TABLE IF NOT EXISTS cars (
  year string,
  make string,
  model string,
  comment string,
  blank string
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
LOCATION 's3://aws-bigdata-blog/artifacts/emr-encryption/data/cars/'
TBLPROPERTIES("skip.header.line.count"="1");

SELECT make, model FROM cars;

CREATE EXTERNAL TABLE IF NOT EXISTS cars_encrypted (
    make string,
    model string
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION 's3://${hiveconf:bucketName}/output/encrypted/cars/';

INSERT OVERWRITE TABLE cars_encrypted
select make, model from cars;
