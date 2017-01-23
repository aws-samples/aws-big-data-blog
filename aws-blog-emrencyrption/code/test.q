DROP TABLE IF EXISTS cf_logs;

CREATE EXTERNAL TABLE IF NOT EXISTS cf_logs (
  date date,
  time string,
  location string,
  bytes bigint,
  requestip string,
  method string,
  host string,
  uri string,
  status bigint,
  referrer string,
  useragent string,
  uriquery string,
  cookie string,
  resulttype string,
  requestid string,
  header string,
  csprotocol string,
  csbytes string,
  timetaken bigint,
  forwardedfor string,
  sslprotocol string,
  sslcipher string,
  responseresulttype string
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
LOCATION 's3://aws-sai-sriparasa/datalake/cf-logs/'
TBLPROPERTIES("skip.header.line.count"="1");

SELECT date,location from cf_logs limit 10;


CREATE EXTERNAL TABLE IF NOT EXISTS cf_logs_location_10 (
    date date,
    location string
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION 's3://your-bucket/output/encrypted/cf-logs_location_kms/';


INSERT OVERWRITE TABLE cf_logs_location_10
select date, location from cf_logs limit 10;
