DROP TABLE IF EXISTS pageviews;

CREATE EXTERNAL TABLE pageviews(
  code STRING,
  page STRING,
  views INT,
  bytes STRING
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ' '
LINES TERMINATED BY '\n'
STORED AS TEXTFILE
LOCATION 's3://aws-bigdata-blog/artifacts/sqoop-blog/';

DROP TABLE IF EXISTS pv_aggregates;

CREATE TABLE  pv_aggregates(
	dt string,
	code string,
	views int
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ' '
LINES TERMINATED BY '\n'
STORED AS TEXTFILE
LOCATION 's3://your-bucket/data/sqoop/hive/pv-aggregates/';

WITH
 q1 as (SELECT DISTINCT split(split(INPUT__FILE__NAME, '/')[7], '-')[1] FROM pageviews),
 q2 as (SELECT code, sum(views) as sm FROM pageviews GROUP BY code ORDER BY sm DESC LIMIT 10)
INSERT OVERWRITE TABLE pv_aggregates
SELECT * FROM q1,q2;
