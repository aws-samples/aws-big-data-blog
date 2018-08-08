# Connecting to Amazon Athena with Federated Identities using Temporary Credentials

Using temporary security credentials ensures that access keys to protected resources in production are not directly hard-coded in the applications. Instead, you rely on AWS Secure Token Service (AWS STS) to generate temporary credentials. 
Temporary security credentials work similar to the long-term access key credentials that your Amazon IAM users can use, with the following differences. These credentials are:
 *  Intended for short-term use only. You can configure these credentials to last for anywhere from a few minutes to several hours. After they expire, AWS no longer recognizes them, or allows any kind of access from API requests made with them.
 *	Not stored with the user, but are generated dynamically and provided to the user when requested. When (or even before) they expire, the user can request new credentials, as long as the user requesting them still has permissions to do so.

We list below some of the typical use cases in which your organization may require federated access to Amazon Athena:  
1.	Running Queries in Amazon Athena while Using Federation via SAML with Active Directory (AD). Your group requires to run queries in Amazon Athena while federating into AWS using SAML with permissions stored in AD.
2.	Enabling Cross-Account Access to Amazon Athena for Users in Your Organization. A member of your group with access to AWS Account “A” needs to run Athena queries in Account “B”.
3.	Enabling Access to Amazon Athena for a Data Application. A data application deployed on an Amazon EC2 instance needs to run Amazon Athena queries via JDBC.



### Pre-requisites


 * Java 8 is installed
 * SQL workbench is installed on your laptop or Windows EC2 instance.(http://www.sql-workbench.net/Workbench-Build123.zip)

#### SQL Workbench Extended Properties for SAML generated credentials

Property | Value
---------------------------|--------------------------------------------------------------------------------------
AwsCredentialsProviderClass|com.amazonaws.athena.jdbc.CustomIAMRoleAssumptionSAMLCredentialsProvider
AwsCredentialsProviderArguments|*access_key_id,secret_access_key,session token*
S3OutputLocation|s3://*bucket where athena results are stored*
LogPath|*local path on laptop or pc where logs are stored*
LogLevel|*LogLevel 1 thru 6*

#### SQL Workbench Extended Properties for Cross-Account Role Access

Property | Value
---------------------------|-----------------------------------------------------------------------
AwsCredentialsProviderClass|com.amazonaws.athena.jdbc.CustomIAMRoleAssumptionCredentialsProvider
AwsCredentialsProviderArguments|*access_key_id,secret_access_key,Cross Account Role ARN*
S3OutputLocation|s3://*bucket where athena results are stored*                          
LogPath|*local path on laptop or pc where logs are stored*
LogLevel|*LogLevel 1 thru 6*

#### SQL Workbench Extended Properties for EC2 Instance role

Property | Value
---------------------------|--------------------------------------------------------------------------------------
AwsCredentialsProviderClass|com.simba.athena.amazonaws.auth.InstanceProfileCredentialsProvider
S3OutputLocation|s3://*bucket where athena results are stored*
LogPath|*local path on laptop or pc where logs are stored*
LogLevel|*LogLevel 1 thru 6*
