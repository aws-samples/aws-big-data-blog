# athena-jdbc

You can use Athena to generate reports or to explore data with business intelligence tools or SQL clients connected with a JDBC driver. To demonstrate the scenarios described above, we will be using SQL Workbench tool which is open source SQL editor. Refer to http://docs.aws.amazon.com/athena/latest/ug/athena-sql-workbench.html for installation and setup steps.  
Athena JDBC driver provides ability to add custom credentials provider. This feature allows to utilize Secure Token Service to obtain temporary credentials.
You can use the AWS Security Token Service (AWS STS) to create and provide trusted users with temporary security credentials that can control access to your AWS resources. Temporary security credentials work almost identically to the long-term access key credentials that your IAM users can use, with the following differences:
 *	Temporary security credentials are short-term, as the name implies. They can be configured to last for anywhere from a few minutes to several hours. After the credentials expire, AWS no longer recognizes them or allows any kind of access from API requests made with them.
 *  Temporary security credentials are not stored with the user but are generated dynamically and provided to the user when requested. When (or even before) the temporary security credentials expire, the user can request new credentials, as long as the user requesting them still has permissions to do so.

### Pre-requisites

 * Java 8 is installed
 * SQL workbench is installed on your laptop or Windows EC2 instance.(http://www.sql-workbench.net/Workbench-Build123.zip)

#### SQL Workbench Extended Properties for Cross-Account Role Access

Property | Value
---------------------------|-----------------------------------------------------------------------
AwsCredentialsProviderClass|com.amazonaws.athena.jdbc.CustomIAMRoleAssumptionCredentialsProvider
AwsCredentialsProviderArguments|*access_key_id,secret_access_key,Cross Account Role ARN*
S3OutputLocation|s3://*bucket where athena results are stored*                          
LogPath|*local path on laptop or pc where logs are stored*
LogLevel|*LogLevel 1 thru 6*

#### SQL Workbench Extended Properties for SAML generated credentials

Property | Value
---------------------------|--------------------------------------------------------------------------------------
AwsCredentialsProviderClass|com.amazonaws.athena.jdbc.CustomIAMRoleAssumptionSAMLCredentialsProvider
AwsCredentialsProviderArguments|*access_key_id,secret_access_key,session token*
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
