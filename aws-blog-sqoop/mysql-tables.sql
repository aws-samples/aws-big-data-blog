CREATE TABLE `pv_aggregates` (
  `dt` varchar(10) DEFAULT NULL,
  `code` varchar(20) DEFAULT NULL,
  `views` int(11) DEFAULT NULL
);

CREATE TABLE pv_aggregates_stg LIKE pv_aggregates;
