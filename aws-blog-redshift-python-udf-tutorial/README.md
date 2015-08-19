# An Introduction to Python UDFs in Amazon Redshift with the CMS Open Payments Dataset
This is the code repository for samples  used the AWS Big Data blog An Introduction to Python UDFs in Amazon Redshift with the CMS Open Payments Dataset

##Overview of example

Amazon Redshift released a new feature that allows Python based User Defined Functions (UDFs) within an Amazon Redshift cluster. This post will serve as a tutorial to get you started using Python UDFs as a tool to accelerate and enhance your analysis as you explore the CMS Open Payments Dataset and make it so you never have to doubt your physician again.

##Description of files

###LOAD_OPPR_ALL_DTL_GNRL_12192014.sql
creates a structure to hold the data in the general payments file. 
Per CMS README
File #1 - OPPR_ALL_DTL_GNRL_12192014.csv: 
This file contains the data set for identified General Payments for the 2013 program year, including the 
additional payment records added to the publication during the data refresh in December 2014. General Payments are defined --as payments or other transfers of value not made in connection with a research agreement or research protocol.

##LOAD_OPPR_ALL_DTL_RSRCH_12192014.sql
Loads file #2 - OPPR_ALL_DTL_RSRCH_12192014.csv: This file contains the data set for identified Research Payments for the 2013 program year,
including the additional payment records added to the publication during the data refresh in December 2014.
Research Payments are defined as payments or other transfers of value made in connection with a research agreement or research protocol.

##LOAD_OPPR_ALL_DTL_OWNRSHP_12192014.sql
Loads file File #3 - OPPR_ALL_DTL_OWNRSHP_12192014.csv: This file contains the data set for identified Ownership
and Investment Interest Information for the 2013 program year, including the additional payment records added to the publication during the data refresh in December 2014. Ownership and Investment Interest Information is defined as information about the value of ownership or investment interest in an applicable manufacturer or applicable group purchasing organization.

##LOAD_OPPR_SPLMTL_PH_PRFL_12192014.sql
Loads File #4 - OPPR_SPLMTL_PH_PRFL_12192014.csv: A supplementary file that displays all of the physicians indicated as recipients of payments,
other transfers of value,or ownership and investment interest in records reported in Open Payments. Each record includes the physician’s demographic information,specialties,and license information,as well as a unique identification number (Physician Profile ID) that can be used to search for a specific physician in the general,research,and physician ownership files.

##f_return_focused_specialty.sql
Example of a Python UDF which preforms a simple transform that returns the focused specialty of a physician. 

##f_return_general_specialty.sql
Example of a Python UDF which preforms a simple transform that returns the general specialty of a physician. 

##f_z_test_by_pval.sql
Scalar Python UDF example for finding a Z-score 

###CMS.STAT_RELEVANT_PHYSICIAN_SPECIALTY.SQL

Creates a Reddshift view that returns the statistical significance of a physician average using a Python UDF for Z-scores. 





