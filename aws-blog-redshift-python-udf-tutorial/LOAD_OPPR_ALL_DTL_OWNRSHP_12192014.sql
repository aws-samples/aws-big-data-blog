--File #3 - OPPR_ALL_DTL_OWNRSHP_12192014.csv: This file contains the data set for identified Ownership
--and Investment Interest Information for the 2013 program year,
--including the additional payment records added to the publication during the data refresh in December 2014. Ownership
--and Investment Interest Information is defined as information about the value of ownership or investment interest in an applicable manufacturer or applicable
--group purchasing organization.

CREATE TABLE CMS.OPPR_ALL_DTL_OWNRSHP_12192014
(
   physician_ownership_transaction_id int,
   program_year int,
   payment_publication_date date,
   submitting_applicable_manufacturer_or_applicable_gpo_name varchar(53),
   physician_profile_id int,
   physician_first_name varchar(20),
   physician_middle_name varchar(15),
   physician_last_name varchar(18),
   physician_name_suffix varchar(6),
   recipient_primary_business_street_address_line1 varchar(56),
   recipient_primary_business_street_address_line2 varchar(48),
   recipient_city varchar(27),
   recipient_state char(2),
   recipient_zip_code varchar(11),
   recipient_country varchar(14),
   recipient_province varchar(2),
   recipient_postal_code varchar(6),
   physician_primary_type varchar(29),
   physician_specialty varchar(118),
   applicable_manufacturer_or_applicable_gpo_making_payment_name varchar(53),
   applicable_manufacturer_or_applicable_gpo_making_payment_id bigint,
   applicable_manufacturer_or_applicable_gpo_making_payment_state char(2),
   applicable_manufacturer_or_applicable_gpo_making_payment_country varchar(14),
   dispute_status_for_publication varchar(4),
   interest_held_by_physician_or_an_immediate_family_member varchar(28),
   dollar_amount_invested float,
   value_of_interest float,
   terms_of_interest varchar(492)
)
distkey(physician_profile_id)
sortkey(physician_profile_id)
;

COPY CMS.OPPR_ALL_DTL_OWNRSHP_12192014 
from 's3://YOUR_S3_BUCKET/OPPR_ALL_DTL_OWNRSHP_12192014.csv'  
REGION 'us-west-2'
CREDENTIALS 'aws_access_key_id=YOUR_KEY_HERE;aws_secret_access_key=YOUR_SECRET_HERE'
IGNOREHEADER 1
DATEFORMAT AS 'MM/DD/YYYY'
csv;




