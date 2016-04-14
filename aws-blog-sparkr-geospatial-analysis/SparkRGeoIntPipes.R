##############################################
#    GeoSpatial Analysis Using SparkR on EMR (Using Pipes)
#    --Alternate way to implement using Pipes 
#         ---By Gopal Wunnava, Senior Consultant, Amazon Web Services
#This code demonstrates how to implement a geospatial use case using SparkR on EMR
#We will use the GDELT dataset for this purpose which is available on EMR
##############################################
#Prerequsite: run "sudo yum install libjpeg-turbo-devel" to load this package which contains file jpeglib.h
#Required R libraries -please ensure they have been installed
#If not done so, install the required packages for creating geospatial application using command below
#install.packages(c("plyr","dplyr","mapproj","RgoogleMaps","ggmap"))
#install.packages("pipeR")

#Save visualization results to pdf below
#you can use setwd to change the path from the default current working directory
setwd("/home/hadoop")
pdf("SparkRGEOINTEMRpipes.pdf")

#plot.new()
library(RgoogleMaps)
library(ggmap)
library(mapproj)
library(plyr)
library(magrittr)
library(pipeR)

hiveContext <- sparkRHive.init(sc)
sql(hiveContext,
"
 CREATE EXTERNAL TABLE IF NOT EXISTS gdelt (
 GLOBALEVENTID BIGINT,
 SQLDATE INT,
 MonthYear INT,
 Year INT,
 FractionDate DOUBLE,
 Actor1Code STRING,
 Actor1Name STRING,
 Actor1CountryCode STRING,
 Actor1KnownGroupCode STRING,
 Actor1EthnicCode STRING,
 Actor1Religion1Code STRING,
 Actor1Religion2Code STRING,
 Actor1Type1Code STRING,
 Actor1Type2Code STRING,
 Actor1Type3Code STRING,
 Actor2Code STRING,
 Actor2Name STRING,
 Actor2CountryCode STRING,
 Actor2KnownGroupCode STRING,
 Actor2EthnicCode STRING,
 Actor2Religion1Code STRING,
 Actor2Religion2Code STRING,
 Actor2Type1Code STRING,
 Actor2Type2Code STRING,
 Actor2Type3Code STRING,
 IsRootEvent INT,
 EventCode STRING,
 EventBaseCode STRING,
 EventRootCode STRING,
 QuadClass INT,
 GoldsteinScale DOUBLE,
 NumMentions INT,
 NumSources INT,
 NumArticles INT,
 AvgTone DOUBLE,
 Actor1Geo_Type INT,
 Actor1Geo_FullName STRING,
 Actor1Geo_CountryCode STRING,
 Actor1Geo_ADM1Code STRING,
 Actor1Geo_Lat FLOAT,
 Actor1Geo_Long FLOAT,
 Actor1Geo_FeatureID INT,
 Actor2Geo_Type INT,
 Actor2Geo_FullName STRING,
 Actor2Geo_CountryCode STRING,
 Actor2Geo_ADM1Code STRING,
 Actor2Geo_Lat FLOAT,
 Actor2Geo_Long FLOAT,
 Actor2Geo_FeatureID INT,
 ActionGeo_Type INT,
 ActionGeo_FullName STRING,
 ActionGeo_CountryCode STRING,
 ActionGeo_ADM1Code STRING,
 ActionGeo_Lat FLOAT,
 ActionGeo_Long FLOAT,
 ActionGeo_FeatureID INT,
 DATEADDED INT,
 SOURCEURL STRING )
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION 's3://support.elasticmapreduce/training/datasets/gdelt'
");

gdelt<-sql(hiveContext,"SELECT * FROM gdelt")
registerTempTable(gdelt, "gdelt")
cacheTable(hiveContext, "gdelt")

#====rename cols for readability
names(gdelt)[names(gdelt)=="actiongeo_countrycode"]="cn"
names(gdelt)[names(gdelt)=="actiongeo_lat"]="lat"
names(gdelt)[names(gdelt)=="actiongeo_long"]="long"
names(gdelt)

#Implementation using Pipes with results saved to native R dataframe 
dflocalpr=filter(gdelt,gdelt$cn == 'US' | gdelt$cn == 'IN') %>>% 
		 filter(gdelt$year >= 2014) %>>% 
		 filter(gdelt$eventcode %in% c("0211","0231","0311","0331","061","071")) %>>% 
		 select("eventcode","cn","year","lat","long") %>>% 
		 collect

#------------------------USE CASE 1-------------------------
#Use Case 1 will identify locations where specific CAMEO events related to economy are taking place
save(dflocalpr, file = "gdeltlocalpr.Rdata")

#filter by US and IN
dflocalus1=subset(dflocalpr,dflocalpr$cn=='US')
dflocalin1=subset(dflocalpr,dflocalpr$cn=='IN')

#Let us display list of unique events on the console
unique(dflocalus1$eventcode)
unique(dflocalin1$eventcode)

#Spatial Analysis of chosen events related to Economy in the US
plot.new()
title("GDELT Analysis for Economy related Events in 2014-2015")
map=qmap('USA',zoom=3)
map + geom_point(data = dflocalus1, aes(x = dflocalus1$long, y = dflocalus1$lat), color="blue", size=0.5, alpha=0.5)
title("All GDELT Event Locations in USA related to Economy in 2014-2015")

#now lets filter based on specific codes, 0211 (Economic Co-ops - Appeals) first
dflocalus0211=subset(dflocalus1,dflocalus1$eventcode=='0211')
x0211=geom_point(data = dflocalus0211, aes(x = dflocalus0211$long, y = dflocalus0211$lat), color="violet", size=2, alpha=0.5)
map+x0211
title("GDELT Event Locations in USA: Economic Co-op(appeals)-Code 0211")

#lets filter further based on specific codes, 0231 (Economic Aid - Appeals) next
dflocalus0231=subset(dflocalus1,dflocalus1$eventcode=='0231')
x0231=geom_point(data = dflocalus0231, aes(x = dflocalus0231$long, y = dflocalus0231$lat), color="orange", size=2, alpha=0.5)
map+x0231
title("GDELT Event Locations in USA:Economic Aid(appeals)-Code 0231")

#To display multiple events on a map, each with a different color
dflocalus0311=subset(dflocalus1,dflocalus1$eventcode=='0311')
dflocalus0331=subset(dflocalus1,dflocalus1$eventcode=='0331')
dflocalus061=subset(dflocalus1,dflocalus1$eventcode=='061')
dflocalus071=subset(dflocalus1,dflocalus1$eventcode=='071')

x0211=geom_point(data = dflocalus0211, aes(x = dflocalus0211$long, y = dflocalus0211$lat), color="violet", size=1, alpha=0.5)
x0231=geom_point(data = dflocalus0231, aes(x = dflocalus0231$long, y = dflocalus0231$lat), color="orange", size=1, alpha=0.5)
x0311=geom_point(data = dflocalus0311, aes(x = dflocalus0311$long, y = dflocalus0311$lat), color="green", size=1, alpha=0.5)
x0331=geom_point(data = dflocalus0331, aes(x = dflocalus0331$long, y = dflocalus0331$lat), color="yellow", size=4, alpha=0.5)
x061=geom_point(data = dflocalus061, aes(x = dflocalus061$long, y = dflocalus061$lat), color="red", size=2, alpha=0.5)
x071=geom_point(data = dflocalus071, aes(x = dflocalus071$long, y = dflocalus071$lat), color="blue", size=0.25, alpha=0.5)

#Let us display 3 chosen events on the Map for the US
map+x0331+x061+x071

legend('bottomleft',c("0331-Intent for Economic Aid","061-Provide Economic Co-operation","071-Provide Economic Aid"),col=c("yellow","red","blue"),pch=16)
title("GDELT Locations In USA: Economy related Events in 2014-2015")

#Spatial Analysis of chosen events in India
plot.new()
title("Locations of chosen GDELT Economic Events in India")
map=qmap('India',zoom=4)

map + geom_point(data = dflocalin1, aes(x = dflocalin1$long, y = dflocalin1$lat), color="red", size=2, alpha=0.5)
title("GDELT Economic Event Analysis in India for 2014-2015")

#now lets filter based on specific codes, 0211 (Economic Co-ops - Appeals) first
dflocalin0211=subset(dflocalin1,dflocalin1$eventcode=='0211')
x0211=geom_point(data = dflocalin0211, aes(x = dflocalin0211$long, y = dflocalin0211$lat), color="blue", size=3, alpha=0.5)
map+x0211
title("GDELT Event Locations in India:Economic Co-op (appeals) - code 0211")

#lets filter further based on specific codes, 0231 (Economic Aid - Appeals) next
dflocalin0231=subset(dflocalin1,dflocalin1$eventcode=='0231')
x0231=geom_point(data = dflocalin0231, aes(x = dflocalin0231$long, y = dflocalin0231$lat), color="yellow", size=3, alpha=0.5)

map+x0231
title("GDELT Event Locations in India: Economic Aid (appeals)-code 0231")

#To display multiple events on a map, each with a different color

dflocalin0311=subset(dflocalin1,dflocalin1$eventcode=='0311')
dflocalin0331=subset(dflocalin1,dflocalin1$eventcode=='0331')
dflocalin061=subset(dflocalin1,dflocalin1$eventcode=='061')
dflocalin071=subset(dflocalin1,dflocalin1$eventcode=='071')

x0231=geom_point(data = dflocalin0231, aes(x = dflocalin0231$long, y = dflocalin0231$lat), color="yellow", size=3, alpha=0.5)
x0311=geom_point(data = dflocalin0311, aes(x = dflocalin0311$long, y = dflocalin0311$lat), color="orange", size=1, alpha=0.5)
x0331=geom_point(data = dflocalin0331, aes(x = dflocalin0331$long, y = dflocalin0331$lat), color="blue", size=2, alpha=0.5)
x061=geom_point(data = dflocalin061, aes(x = dflocalin061$long, y = dflocalin061$lat), color="orange", size=1, alpha=0.5)
x071=geom_point(data = dflocalin071, aes(x = dflocalin071$long, y = dflocalin071$lat), color="red", size=1, alpha=0.5)
map+x0231+x0331+x071

legend('topright',c("0231:Appeal for Economic Aid","0331:Intent for Economic Aid","071:Provide Economic Aid"),col=c("yellow","blue","red"),pch=16)

title("GDELT Event Locations in India:Economic Events in Years 2014-2015")


#------------------------USE CASE 2-------------------------
#Use Case 2 will identify location with highest density/frequency of CAMEO events related to economy

df <- createDataFrame(sqlContext, dflocalpr)
df %>>%
   group_by(df$cn,df$lat,df$long,df$year) %>>%
   agg(eventcount = n(df$eventcode)) %>>%
   collect ->dfcr
#Let us save results locally
save(dfcr, file = "gdeltdfcr.Rdata")

#filter by US and IN
dflocalus2=subset(dfcr,dfcr$cn=='US')
dflocalin2=subset(dfcr,dfcr$cn=='IN')

dfustop=head(arrange(dflocalus2,desc(totevents)),50)
dfintop=head(arrange(dflocalin2,desc(totevents)),50)
dfustop
dfintop

map=qmap('USA',zoom=3)
map + geom_point(data = dfustop, aes(x = dfustop$long, y = dfustop$lat), color="blue", size=3, alpha=0.5)
title("GDELT Economic Event Analysis-USA: Top 50 Most Frequent Locations")

#lets plot on map for IN
map=qmap('India',zoom=4)
map + geom_point(data = dfintop, aes(x = dfintop$long, y = dfintop$lat), color="blue", size=3, alpha=0.5)
title("GDELT Economic Event Analysis-India:Top 50 Most Frequent Locations")
dev.off()
sparkR.stop()
ls()
###################Code Ends###########################