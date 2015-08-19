--File #2 - OPPR_ALL_DTL_RSRCH_12192014.csv: This file contains the data set for identified Research Payments for the 2013 program year,
--including the additional payment records added to the publication during the data refresh in December 2014.
--Research Payments are defined as payments or other transfers of value made in connection with a research agreement or research protocol.

create TABLE CMS.OPPR_ALL_DTL_RSRCH_12192014
(
   research_transaction_id int,
   program_year int,
   payment_publication_date date,
   submitting_applicable_manufacturer_or_applicable_gpo_name varchar(79),
   covered_recipient_type varchar(36),
   noncovered_recipient_entity_name varchar(1),
   teaching_hospital_id int,
   teaching_hospital_name varchar(71),
   physician_profile_id int,
   physician_first_name varchar(16),
   physician_middle_name varchar(17),
   physician_last_name varchar(23),
   physician_name_suffix varchar(6),
   recipient_primary_business_street_address_line1 varchar(56),
   recipient_primary_business_street_address_line2 varchar(56),
   recipient_city varchar(26),
   recipient_state char(2),
   recipient_zip_code varchar(11),
   recipient_country varchar(14),
   recipient_province varchar(1),
   recipient_postal_code varchar(6),
   physician_primary_type varchar(29),
   physician_specialty varchar(118),
   physician_license_state_code1 varchar(3),
   physician_license_state_code2 varchar(3),
   physician_license_state_code3 varchar(3),
   physician_license_state_code4 varchar(1),
   physician_license_state_code5 varchar(1),
   product_indicator varchar(12),
   name_of_associated_drug_or_biological1 varchar(59),
   name_of_associated_drug_or_biological2 varchar(31),
   name_of_associated_drug_or_biological3 varchar(20),
   name_of_associated_drug_or_biological4 varchar(37),
   name_of_associated_drug_or_biological5 varchar(20),
   ndc_of_associated_covered_drug_or_biological1 varchar(13),
   ndc_of_associated_covered_drug_or_biological2 varchar(13),
   ndc_of_associated_covered_drug_or_biological3 varchar(13),
   ndc_of_associated_covered_drug_or_biological4 varchar(13),
   ndc_of_associated_covered_drug_or_biological5 varchar(13),
   name_of_associated_covered_device_or_medical_supply1 varchar(81),
   name_of_associated_covered_device_or_medical_supply2 varchar(43),
   name_of_associated_covered_device_or_medical_supply3 varchar(43),
   name_of_associated_covered_device_or_medical_supply4 varchar(22),
   name_of_associated_covered_device_or_medical_supply5 varchar(17),
   applicable_manufacturer_or_applicable_gpo_making_payment_id bigint,
   applicable_manufacturer_or_applicable_gpo_making_payment_name varchar(79),
   applicable_manufacturer_or_applicable_gpo_making_payment_state char(2),
   applicable_manufacturer_or_applicable_gpo_making_payment_country varchar(19),
   dispute_status_for_publication varchar(4),
   total_amount_of_payment_usdollars float,
   date_of_payment date,
   form_of_payment_or_transfer_of_value varchar(27),
   expenditure_category1 varchar(40),
   expenditure_category2 varchar(6),
   expenditure_category3 varchar(1),
   expenditure_category4 varchar(1),
   expenditure_category5 varchar(1),
   preclinical_research_indicator varchar(4),
   delay_in_publication_of_research_payment_indicator varchar(3),
   name_of_study varchar(501),
   principal_investigator_name1 varchar(34),
   principal_investigator_name2 varchar(24),
   principal_investigator_name3 varchar(1),
   principal_investigator_name4 varchar(1),
   principal_investigator_name5 varchar(1),
   clinicaltrials_gov_identifier varchar(12),
   research_information_link varchar(85),
   context_of_research varchar(473)
)
distkey(physician_profile_id)
sortkey(physician_profile_id)
;

COPY CMS.OPPR_ALL_DTL_RSRCH_12192014 
from 's3://YOUR_S3_BUCKET/OPPR_ALL_DTL_RSRCH_12192014.csv'  
REGION 'us-west-2'
CREDENTIALS 'aws_access_key_id=YOUR_KEY_HERE;aws_secret_access_key=YOUR_KEY_HERE'
IGNOREHEADER 1
DATEFORMAT AS 'MM/DD/YYYY'
csv;
