--creates a structure to hold the data in the general payments file. 
--Per CMS README
--File #1 - OPPR_ALL_DTL_GNRL_12192014.csv: 
--This file contains the data set for identified General Payments for the 2013 program year, including the 
--additional payment records added to the publication during the data refresh in December 2014. General Payments are defined --as payments or other transfers of value not made in connection with a research agreement or research protocol.

CREATE TABLE CMS.OPPR_ALL_DTL_GNRL_12192014
(
   general_transaction_id integer,
   program_year integer,
   payment_publication_date date,
   submitting_applicable_manufacturer_or_applicable_gpo_name varchar(101),
   covered_recipient_type varchar(36),
   teaching_hospital_id varchar(5),
   teaching_hospital_name varchar(71),
   physician_profile_id varchar(7),
   physician_first_name varchar(21),
   physician_middle_name varchar(21),
   physician_last_name varchar(36),
   physician_name_suffix varchar(6),
   recipient_primary_business_street_address_line1 varchar(56),
   recipient_primary_business_street_address_line2 varchar(56),
   recipient_city varchar(41),
   recipient_state char(2),
   recipient_zip_code varchar(11),
   recipient_country varchar(37),
   recipient_province varchar(17),
   recipient_postal_code varchar(11),
   physician_primary_type varchar(29),
   physician_specialty varchar(135),
   physician_license_state_code1 char(2),
   physician_license_state_code2 char(2),
   physician_license_state_code3 char(2),
   physician_license_state_code4 char(2),
   physician_license_state_code5 char(2),
   product_indicator varchar(12),
   name_of_associated_covered_drug_or_biological1 varchar(77),
   name_of_associated_covered_drug_or_biological2 varchar(77),
   name_of_associated_covered_drug_or_biological3 varchar(77),
   name_of_associated_covered_drug_or_biological4 varchar(66),
   name_of_associated_covered_drug_or_biological5 varchar(90),
   ndc_of_associated_covered_drug_or_biological1 varchar(13),
   ndc_of_associated_covered_drug_or_biological2 varchar(13),
   ndc_of_associated_covered_drug_or_biological3 varchar(13),
   ndc_of_associated_covered_drug_or_biological4 varchar(13),
   ndc_of_associated_covered_drug_or_biological5 varchar(13),
   name_of_associated_covered_device_or_medical_supply1 varchar(101),
   name_of_associated_covered_device_or_medical_supply2 varchar(101),
   name_of_associated_covered_device_or_medical_supply3 varchar(101),
   name_of_associated_covered_device_or_medical_supply4 varchar(101),
   name_of_associated_covered_device_or_medical_supply5 varchar(101),
   applicable_manufacturer_or_applicable_gpo_making_payment_name varchar(101),
   applicable_manufacturer_or_applicable_gpo_making_payment_id varchar(13),
   applicable_manufacturer_or_applicable_gpo_making_payment_state varchar(3),
   applicable_manufacturer_or_applicable_gpo_making_payment_country varchar(19),
   dispute_status_for_publication varchar(4),
   total_amount_of_payment_usdollars float,
   date_of_payment date,
   number_of_payments_included_in_total_amount varchar(4),
   form_of_payment_or_transfer_of_value varchar(53),
   nature_of_payment_or_transfer_of_value varchar(147),
   city_of_travel varchar(30),
   state_of_travel char(2),
   country_of_travel varchar(37),
   physician_ownership_indicator varchar(4),
   third_party_payment_recipient_indicator varchar(23),
   name_of_third_party_entity_receiving_payment_or_transfer_of_value varchar(51),
   charity_indicator varchar(4),
   third_party_equals_covered_recipient_indicator varchar(4),
   contextual_information varchar(478),
   delay_in_publication_of_general_payment_indicator varchar(3)
)
distkey(physician_profile_id)
sortkey(physician_profile_id)
;

---loads 2.6+ million records. May take between 4 and 10 minutes depending on underlying cluster size
COPY CMS.OPPR_ALL_DTL_GNRL_12192014 
from 's3://YOUR_S3_BUCKET/OPPR_ALL_DTL_GNRL_12192014.csv'  
REGION 'us-west-2'
CREDENTIALS 'aws_access_key_id=YOUR_KEY_HERE;aws_secret_access_key=YOUR_SECRET_HERE'
IGNOREHEADER 1
DATEFORMAT AS 'MM/DD/YYYY'
csv;

--on error, use this query to find last load record that caused error:
--select * from stl_load_errors
--WHERE starttime = (select max(starttime) from stl_load_errors)



