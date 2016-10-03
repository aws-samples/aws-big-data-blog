create table edbaconfig.ingestedfilestatus(
file_url varchar(500) primary key,
submitted_jobs json,
last_update_status varchar(50),
last_validated_timestamp timestamp,
unique(file_url,last_update_status)
);

create table edbaconfig.aggrjobconfiguration(
job_config_id varchar(10) primary key,
job_input_pattern varchar(500),
job_min_file_count int,
job_addl_criteria varchar(1000),
job_params varchar(1000),
last_exec_stepid varchar(50),
last_exec_status varchar(20),
last_run_timestamp timestamp
);

--Before inserting the below two configurations, replace <<S3_EDBA_BUCKET>> with the S3 bucket you created in step#1 and <<S3_CONF_FILE_PREFIX>> with the prefix to your edba-spark.conf file
insert into edbaconfig.aggrjobconfiguration(job_config_id,job_input_pattern,job_min_file_count,job_addl_criteria,job_params,last_exec_stepid,last_exec_status,last_run_timestamp)
 values('J101','ingestedfilestatus.file_url like \'%validated%IL%.csv\'',13,'select 1 from ingestedfilestatus where file_url like \'%Item%.csv\' and last_validated_timestamp > current_timestamp - interval 1 day','spark-submit,--deploy-mode,cluster,--class,com.amazonaws.bigdatablog.edba.emr.ProcessVendorTrasactions,s3://<<S3_EDBA_BUCKET>>/code/eventdrivenanalytics.jar,<<S3_EDBA_BUCKET>>,<<S3_CONF_FILE_PREFIX>>,s3://<<S3_EDBA_BUCKET>>/validated/data/source-identical/IL*.csv',null,null,null);

insert into edbaconfig.aggrjobconfiguration(job_config_id,job_input_pattern,job_min_file_count,job_addl_criteria,job_params,last_exec_stepid,last_exec_status,last_run_timestamp)
 values('J102','ingestedfilestatus.file_url like \'%validated%CA%.csv\'',25,'select 1 from ingestedfilestatus where file_url like \'%Item%.csv\' and last_validated_timestamp > current_timestamp - interval 1 day','spark-submit,--deploy-mode,cluster,—-class,com.amazonaws.bigdatablog.edba.emr.ProcessVendorTrasactions,s3://<<S3_EDBA_BUCKET>>/code/eventdrivenanalytics.jar,<<S3_EDBA_BUCKET>>,<<S3_CONF_FILE_PREFIX>>,s3://<<S3_EDBA_BUCKET>>/validated/data/source-identical/CA*.csv',null,null,null);
