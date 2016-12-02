import groovy.sql.Sql

def dbDriver = 'com.amazonaws.athena.jdbc.AthenaDriver'
def dbUrl = 'jdbc:awsathena://athena.us-east-1.amazonaws.com:443/awsdatacatalog/'

java.util.Properties props = new Properties();

//script parameters *** Modify as needed ***
props.put("s3_staging_dir", "s3://ws-athena-query-results-959874710265/staging/");
props.put("user", "<<access key id>>");
props.put("password", "<<secret access key>>");

if (args.size() < 2){
  println "\nUsage: groovy executescript.py <<hive script file>> <<target database>>"
  System.exit(0)
}
def targetfile = new File( args[1] )
if( !targetfile.exists() ) {
  println "Error: File does not exist"
  System.exit(0)
}
dataList = targetfile.text.split( ';' )
print "\nFound "+(dataList.size()-1)+" statements in script...\n"
sql=Sql.newInstance( dbUrl+args[0], props, dbDriver)
dataList.eachWithIndex {elem, i ->
   if (elem.trim()!=";" && elem.trim().size()>0){
      println "\n"+(i+1)+". Executing :"+elem

      try{
         //execute sql statement
         row = sql.firstRow(elem)
         if (row==null) {
            print "\nresult : OK\n\n"
         } else {
            print "\nresult : " +row+"\n\n"
         }
      }
      catch(Exception ex) {
         println("Caught exception: "+ex);
      }
   }
}
