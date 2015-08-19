--File #4 - OPPR_SPLMTL_PH_PRFL_12192014.csv: A supplementary file that displays all of the physicians indicated as recipients of payments,
--other transfers of value,
--or ownership
--and investment interest in records reported in Open Payments. Each record includes the physician’s demographic information,
--specialties,
--and license information,
--as well as a unique identification number (Physician Profile ID) that can be used to search for a specific physician in the general,
--research,
--and physician ownership files.


CREATE TABLE CMS.OPPR_SPLMTL_PH_PRFL_12192014
(
   physician_id int,
   physician_profile_id int,
   physician_profile_first_name varchar(21),
   physician_profile_middle_name varchar(21),
   physician_profile_last_name varchar(29),
   physician_profile_suffix_name varchar(5),
   physician_profile_address1 varchar(56),
   physician_profile_address2 varchar(56),
   physician_profile_city varchar(41),
   physician_profile_state char(2),
   physician_profile_zip_code varchar(6),
   physician_profile_country varchar(37),
   physician_profile_province varchar(1),
   physician_registration_address1 varchar(56),
   physician_registration_address2 varchar(56),
   physician_registration_city varchar(25),
   physician_registration_state char(2),
   physician_registration_zip_code varchar(11),
   physician_registration_country varchar(14),
   physician_registration_province varchar(1),
   physician_specialty varchar(135),
   physician_additional_specialty1 varchar(126),
   physician_additional_specialty2 varchar(118),
   physician_additional_specialty3 varchar(118),
   physician_additional_specialty4 varchar(118),
   physician_additional_specialty5 varchar(81),
   physician_states_on_licenses1 char(2),
   physician_states_on_licenses2  char(2),
   physician_states_on_licenses3  char(2),
   physician_states_on_licenses4 char(2),
   physician_states_on_licenses5  char(2)
)
distkey(physician_profile_id)
sortkey(physician_profile_id)
;

COPY CMS.OPPR_SPLMTL_PH_PRFL_12192014
from 's3://YOUR_S3_BUCKET/OPPR_SPLMTL_PH_PRFL_12192014.csv'  
REGION 'us-west-2'
CREDENTIALS 'aws_access_key_id=YOURKEYHERE;aws_secret_access_key=YOURSECRETHERE'
IGNOREHEADER 1
DATEFORMAT AS 'MM/DD/YYYY'
csv;
