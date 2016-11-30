import groovy.sql.Sql

// DB Settings
def dbDriver = 'com.amazonaws.athena.jdbc.AthenaDriver'
def dbUrl = 'jdbc:awsathena://athena.us-east-1.amazonaws.com:443'

java.util.Properties props = new Properties();

//script parameters *** Modify as needed ***
props.put("s3_staging_dir", "s3://ws-athena-query-results-<account number>/staging/");
props.put("user", "<access key id>");
props.put("password", "<secret access key>");
def tablename='default.elb_logs_raw_native_part' //database and table location
def s3location='s3://athena-examples/elb/raw/'
def startdate = Date.parse("yyyy-MM-dd", "2015-01-01")
def enddate = Date.parse("yyyy-MM-dd", "2015-12-31")
// end of script parameters 

sql = Sql.newInstance( dbUrl, props, dbDriver)

startdate.upto(enddate){
   //build statement
   statement="ALTER TABLE "+tablename+" ADD PARTITION (year="+it[Calendar.YEAR]+",month="+(it[Calendar.MONTH].toInteger()+1)+",day="+it[Calendar.DATE]+") location '"+s3location+it[Calendar.YEAR]+"/"+(it[Calendar.MONTH].toInteger()+1)+"/"+it[Calendar.DATE]+"'"
   println "Executing : "+statement+"\n"
   
   //execute statement
   row = sql.firstRow(statement)
   print "result : " +row+"\n\n"
}
