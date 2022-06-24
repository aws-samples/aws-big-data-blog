-- Lets make sure table does not exist
drop table all_flights

-- Create a database table with data for all flights in December 2013
create table all_flights
(
ORD_DELAY_ID        bigint identity(0,1),
YEAR                smallint,
QUARTER	            smallint,
MONTH               smallint,
DAY_OF_MONTH        smallint,
DAY_OF_WEEK	        smallint,
FL_DATE	            date,
UNIQUE_CARRIER      varchar(10),
AIRLINE_ID          int,
CARRIER	            varchar(4),
TAIL_NUM            varchar(8),
FL_NUM              varchar(4),
ORIGIN_AIRPORT_ID   smallint,
ORIGIN              varchar(5),
ORIGIN_CITY_NAME    varchar(35),
ORIGIN_STATE_ABR    varchar(2),
ORIGIN_STATE_NM     varchar(50),
ORIGIN_WAC          varchar(2),
DEST_AIRPORT_ID	    smallint,
DEST                varchar(5),
DEST_CITY_NAME      varchar(35),
DEST_STATE_ABR      varchar(2),
DEST_STATE_NM       varchar(50),
DEST_WAC            varchar(2),
CRS_DEP_TIME        smallint,
DEP_TIME            varchar(6),
DEP_DELAY           numeric(22,6),
DEP_DELAY_NEW       numeric(22,6),
DEP_DEL15           numeric(22,6),
DEP_DELAY_GROUP	    smallint,
DEP_TIME_BLK        varchar(15),
TAXI_OUT            numeric(22,6),
TAXI_IN             numeric(22,6),
CRS_ARR_TIME        numeric(22,6),
ARR_TIME            varchar(6),
ARR_DELAY           numeric(22,6),
ARR_DELAY_NEW       numeric(22,6),
ARR_DEL15           numeric(22,6),
ARR_DELAY_GROUP     smallint,
ARR_TIME_BLK        varchar(15),
CANCELLED           numeric(22,6),
DIVERTED            numeric(22,6),
CRS_ELAPSED_TIME    numeric(22,6),
ACTUAL_ELAPSED_TIME numeric(22,6),
AIR_TIME            numeric(22,6),
FLIGHTS	            numeric(22,6),
DISTANCE            numeric(22,6),
DISTANCE_GROUP      numeric(22,6),
CARRIER_DELAY       numeric(22,6),
WEATHER_DELAY       numeric(22,6),
NAS_DELAY           numeric(22,6),
SECURITY_DELAY      numeric(22,6),
LATE_AIRCRAFT_DELAY numeric(22,6),
primary key (ord_delay_id)
);

-- Copy all flights data for Dec 2013 and 2014 from S3 bucket
copy all_flights
FROM 's3://<path-to-flights-data/<flight-data>.csv'
     credentials 'aws_iam_role=arn:aws:iam::<your-aws-account-number>:role/<role-name>' csv IGNOREHEADER 1;

-- records in all_flights
select count(*) from all_flights;
select top 50 * from all_flights;

drop function f_days_from_holiday (year int, month int, day int);

-- Create Python UDF to compute number of days before/after the nearest holiday
create or replace function f_days_from_holiday (year int, month int, day int)
  returns int
stable
as $$
  import datetime
  from datetime import date
  from calendar import monthrange

  fdate = date(year, month, day)
  last_day_of_month = monthrange(year, month)[1]

  fmt = '%Y-%m-%d'
  s_date = date(year, month, 1)
  e_date = date(year, month, monthrange(year, month)[1])
  start_date = s_date.strftime(fmt)
  end_date = e_date.strftime(fmt)

  """
  Compute a list of holidays over this period
  """
  from pandas.tseries.holiday import USFederalHolidayCalendar
  calendar = USFederalHolidayCalendar()
  holidays = calendar.holidays(start_date, end_date)
  days_from_closest_holiday = [(abs(fdate - hdate)).days for hdate in holidays.date.tolist()]
  return min(days_from_closest_holiday)
$$ language plpythonu;

--
-- IMPORTANT NOTES:
--     1. ord_flights is a table that will be copied to your Redshift cluster from your Spark environment
--     2. Make sure the table exists before issuing the following commands
--
select count(*) from ord_flights;

-- Derived table: training data table for Amazon ML predictive model
create table train_ord_flights
as
        (select * from ord_flights where year = 2013);

select count(*) from train_ord_flights;

-- Derived table: test data table for Amazon ML's batch predictions
create table test_ord_flights
as
        (select * from ord_flights where year = 2013);

select count(*) from test_ord_flights;

