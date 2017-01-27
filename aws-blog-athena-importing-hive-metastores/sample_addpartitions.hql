ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='01') location 's3://athena-examples/elb/raw/2015/01/01';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='02') location 's3://athena-examples/elb/raw/2015/01/02';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='03') location 's3://athena-examples/elb/raw/2015/01/03';
ALTER TABLE default.elb_logs_raw_native_part ADD PARTITION (year='2015',month='01',day='04') location 's3://athena-examples/elb/raw/2015/01/04';
