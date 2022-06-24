-- Create table weather
CREATE EXTERNAL TABLE IF NOT EXISTS w (
  station       string,
  station_name  string,
  elevation     string,
  latitude      string,
  longitude     string,
  wdate         string,
  prcp          decimal(5,1),
  snow          int,
  tmax          string,
  tmin          string,
  awnd          string
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n'
LOCATION 's3://<path-to-weather-dataset>/'
TBLPROPERTIES("skip.header.line.count"="1");

-- Make sure to change wdate to 'date type' - helpful when joining weather with other tables on date
CREATE TABLE weather AS SELECT station, station_name, elevation, latitude, longitude,
                   cast(concat(
                                substr(wdate,1,4), '-',
                                substr(wdate,5,2), '-',
                                substr(wdate,7,2)
                              ) AS date) AS dt, prcp, snow, tmax, tmin, awnd FROM w;

set hive.cli.print.header=true;

select * from weather limit 10;
