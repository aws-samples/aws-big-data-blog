## Cross cluster - Diagnostic queries

The following diagnostic queries are Amazon Athena equivalent of the [diagnostic queries](https://docs.aws.amazon.com/redshift/latest/dg/diagnostic-queries-for-query-tuning.html) provided in Amazon Redshift documentation

#### Top queries across Amazon Redshift clusters

The following query identifies the top 50 most time-consuming statements that have been executed in the last 7 days across all the Amazon Redshift clusters for which the export solution is enabled. Cross cluster equivalent of this [query](https://docs.aws.amazon.com/redshift/latest/dg/diagnostic-queries-for-query-tuning.html#identify-queries-that-are-top-candidates-for-tuning)

```
select clusterid,trim(database) as db, count(query) as n_qry,
max(substring (qrytext,1,80)) as qrytext,
min(run_minutes) as "min" ,
max(run_minutes) as "max",
avg(run_minutes) as "avg", sum(run_minutes) as total,  
max(query) as max_query_id,
cast(max(starttime) as date) as last_run,
sum(alerts) as alerts, aborted
from (select ext_stl_query.clusterid, userid, label, ext_stl_query.query,
trim(database) as database,
trim(querytxt) as qrytext,
md5(to_utf8(trim(querytxt))) as qry_md5,
starttime, endtime,
cast(date_diff('second', starttime,endtime) as decimal(12,2))/60 as run_minutes,     
alrt.num_events as alerts, aborted
from ext_stl_query
left outer join
(select clusterid, query, 1 as num_events from ext_stl_alert_event_log group by clusterid, query ) as alrt
on alrt.clusterid= ext_stl_query.clusterid and alrt.query = ext_stl_query.query
where userid <> 1 and starttime >=  current_date - interval '7' day)
group by clusterid, database, label, qry_md5, aborted
order by last_run desc limit 50
```

#### Tables with Missing Statistics across Amazon Redshift clusters

Cross cluster equivalent of this [query](https://docs.aws.amazon.com/redshift/latest/dg/diagnostic-queries-for-query-tuning.html#identify-tables-with-missing-statistics)

```
select clusterid,substring(trim(plannode),1,100) as plannode, count(*)
from ext_stl_explain
where plannode like '%missing statistics%'
group by plannode,clusterid
order by 2 desc
```

#### Reviewing Queue Wait Times for Queries across Amazon Redshift Clusters

Cross cluster equivalent of this [query](https://docs.aws.amazon.com/redshift/latest/dg/diagnostic-queries-for-query-tuning.html#review-queue-wait-times-for-queries)

```
select w.clusterid,trim(database) as DB , w.query,
substring(q.querytxt, 1, 100) as querytxt,  w.queue_start_time,
w.service_class as class, w.slot_count as slots,
w.total_queue_time/1000000 as queue_seconds,
w.total_exec_time/1000000 exec_seconds, (w.total_queue_time+w.total_Exec_time)/1000000 as total_seconds
from ext_stl_wlm_query w
left join ext_stl_query q on w.clusterid=q.clusterid and q.query = w.query and q.userid = w.userid
where w.queue_start_Time >=  current_date - interval '7' day
and w.total_queue_Time > 0  and w.userid >1   
and q.starttime >=  current_date - interval '7' day
order by w.total_queue_time desc, w.queue_start_time desc limit 35;

```
#### Reviewing Query Alerts by Table across multiple Amazon Redshift clusters

```
select l.clusterid,trim(s.perm_table_name) as "table",
 cast(sum(abs(date_diff('second', s.starttime, s.endtime)))/60 as integer)  as minutes,
 trim(split_part(l.event,':',1)) as event,
trim(l.solution) as solution,
max(l.query) as sample_query, count(*)
from ext_stl_alert_event_log as l
left join ext_stl_scan as s on s.clusterid = l.clusterid and s.query = l.query and s.slice = l.slice
and s.segment = l.segment and s.step = l.step
where l.event_time >=  current_date - interval '7' day
group by 1,2,4,5
order by 3 desc,7 desc;
```

#### Identifying Queries with Nested Loops across multiple Amazon Redshift clusters
cross cluster equivalent of this [query](https://docs.aws.amazon.com/redshift/latest/dg/diagnostic-queries-for-query-tuning.html#identify-queries-with-nested-loops)


```
select clusterid, query, trim(querytxt) as SQL, starttime
from ext_stl_query
where query in (
select distinct query
from ext_stl_alert_event_log
where event like 'Nested Loop Join in the query plan%')
order by starttime desc;
```
