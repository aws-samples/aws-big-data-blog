##############################################
#    GeoSpatial Analysis Using SparkR on EMR
#         ---By Gopal Wunnava, Senior Consultant, Amazon Web Services
#This code demonstrates how to implement a geospatial use case using SparkR on EMR
#We will use the GDELT dataset for this purpose which is available on EMR
##############################################
#Prerequisite: run "sudo yum install libjpeg-turbo-devel" to load this package which contains file jpeglib.h
#Required R libraries -please ensure they have been installed
#If not done so, install the required packages for creating geospatial application using command below
#install.packages(c("plyr","dplyr","mapproj","RgoogleMaps","ggmap"))

#Save visualization results to pdf below
#you can use setwd to change the path from the default current working directory
setwd("/home/hadoop")
pdf("SparkRGEOINTEMR.pdf")
#load the required packages
library(RgoogleMaps)
library(ggmap)
library(mapproj)
library(plyr)

#set up Hive Context
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

gdelt<-sql(hiveContext,"SELECT * FROM gdelt where ActionGeo_CountryCode IN ('IN','US') AND Year >= 2014")

registerTempTable(gdelt, "gdelt")
cacheTable(hiveContext, "gdelt")

#====rename cols for readability
names(gdelt)[names(gdelt)=="actiongeo_countrycode"]="cn"
names(gdelt)[names(gdelt)=="actiongeo_lat"]="lat"
names(gdelt)[names(gdelt)=="actiongeo_long"]="long"
names(gdelt)

#------------------------USE CASE 1-------------------------
#use Case 1 identifies where specific events of interest are taking place on a map(based on eventcode)
gdt=gdelt[,
			 c("sqldate",
			 "eventcode",
			 "globaleventid", 
			 "cn",
	         "year",
	         "lat",
	         "long")
		     ]
registerTempTable(gdt, "gdt")
cacheTable(hiveContext, "gdt")

#Filter on specific events from above to focus on events of interest related to Economy
#For this puprose, we will select CAMEO codes that pertain to economy
#Refer to http://data.gdeltproject.org/documentation/CAMEO.Manual.1.1b3.pdf for details on these codes
ecocodes <- c("0211","0231","0311","0331","061","071")
gdeltusinf <- filter(gdt,gdt$eventcode %in% ecocodes)
registerTempTable(gdeltusinf, "gdeltusinf")
cacheTable(hiveContext, "gdeltusinf")

dflocale1=collect(select(gdeltusinf,"*"))
#save native R dataframe results locally
save(dflocale1, file = "gdeltlocale1.Rdata")

#Now filter these events by country (US and India) and plot on map
dflocalus1=subset(dflocale1,dflocale1$cn=='US')
dflocalin1=subset(dflocale1,dflocale1$cn=='IN')

#get list of unique economy related CAMEO codes in US and India
unique(dflocalus1$eventcode)
unique(dflocalin1$eventcode)

#Spatial Analysis of chosen events related to Economy in the US
plot.new()
title("GDELT Analysis for Economy related Events in 2014-2015")
map=qmap('USA',zoom=3)

map + geom_point(data = dflocalus1, aes(x = dflocalus1$long, y = dflocalus1$lat), color="red", size=0.5, alpha=0.5)
title("All GDELT Event Locations in USA related to Economy in 2014-2015")

#now lets filter based on specific codes, 0211 (Economic Co-op for Appeals) first
dflocalus0211=subset(dflocalus1,dflocalus1$eventcode=='0211')
x0211=geom_point(data = dflocalus0211, aes(x = dflocalus0211$long, y = dflocalus0211$lat), color="blue", size=2, alpha=0.5)

map+x0211
title("GDELT Event Locations in USA: Economic Co-op(appeals)-Code 0211")

#lets filter further based on specific codes, 0231 (Economic Aid for Appeals) next
dflocalus0231=subset(dflocalus1,dflocalus1$eventcode=='0231')
x0231=geom_point(data = dflocalus0231, aes(x = dflocalus0231$long, y = dflocalus0231$lat), color="yellow", size=2, alpha=0.5)

map+x0231
title("GDELT Event Locations in USA:Economic Aid(appeals)-Code 0231")

#To display multiple events on a map, each with a different color

dflocalus0311=subset(dflocalus1,dflocalus1$eventcode=='0311')
dflocalus0331=subset(dflocalus1,dflocalus1$eventcode=='0331')
dflocalus061=subset(dflocalus1,dflocalus1$eventcode=='061')
dflocalus071=subset(dflocalus1,dflocalus1$eventcode=='071')

x0211=geom_point(data = dflocalus0211, aes(x = dflocalus0211$long, y = dflocalus0211$lat), color="blue", size=3, alpha=0.5)
x0231=geom_point(data = dflocalus0231, aes(x = dflocalus0231$long, y = dflocalus0231$lat), color="yellow", size=1, alpha=0.5)
x0311=geom_point(data = dflocalus0311, aes(x = dflocalus0311$long, y = dflocalus0311$lat), color="red", size=1, alpha=0.5)
x0331=geom_point(data = dflocalus0331, aes(x = dflocalus0331$long, y = dflocalus0331$lat), color="green", size=1, alpha=0.5)
x061=geom_point(data = dflocalus061, aes(x = dflocalus061$long, y = dflocalus061$lat), color="orange", size=1, alpha=0.5)
x071=geom_point(data = dflocalus071, aes(x = dflocalus071$long, y = dflocalus071$lat), color="violet", size=1, alpha=0.5)

#Now let us display locations of 3 chosen CAMEO events related to economy in the US
map+x0211+x0231+x0311

legend('bottomleft',c("0211:Appeal for Economic Co-op","0231:Appeal for Economic Aid","0311:Intent for Economic Co-op"),col=c("blue","yellow","red"),pch=16)
title("GDELT Locations In USA: Economy related Events in 2014-2015")

#Spatial Analysis of chosen events in India
plot.new()
title("GDELT Analysis in India: Events related to Economy in 2014-2015")
#title("Following Map will show locations of chosen GDELT Events in India")
map=qmap('India',zoom=4)
map + geom_point(data = dflocalin1, aes(x = dflocalin1$long, y = dflocalin1$lat), color="red", size=1, alpha=0.5)
title("GDELT Event Locations in India:Events related to Economy")

#now lets filter based on specific codes, 0211 (Economic Co-op for Appeals) first
dflocalin0211=subset(dflocalin1,dflocalin1$eventcode=='0211')
x0211=geom_point(data = dflocalin0211, aes(x = dflocalin0211$long, y = dflocalin0211$lat), color="violet", size=3, alpha=0.5)
map+x0211
title("GDELT Event Locations in India:Economic Co-op (appeals) - code 0211")

#lets filter further based on specific codes, 0231 (Economic Aid for Appeals) next
dflocalin0231=subset(dflocalin1,dflocalin1$eventcode=='0231')
x0231=geom_point(data = dflocalin0231, aes(x = dflocalin0231$long, y = dflocalin0231$lat), color="yellow", size=3, alpha=0.5)
map+x0231
title("GDELT Event Locations in India: Economic Aid (appeals)-code 0231")

#To display multiple events on a map, each with a different color
dflocalin0311=subset(dflocalin1,dflocalin1$eventcode=='0311')
dflocalin0331=subset(dflocalin1,dflocalin1$eventcode=='0331')
dflocalin061=subset(dflocalin1,dflocalin1$eventcode=='061')
dflocalin071=subset(dflocalin1,dflocalin1$eventcode=='071')


x0311=geom_point(data = dflocalin0311, aes(x = dflocalin0311$long, y = dflocalin0311$lat), color="orange", size=1, alpha=0.5)
x0331=geom_point(data = dflocalin0331, aes(x = dflocalin0331$long, y = dflocalin0331$lat), color="blue", size=3, alpha=0.5)
x061=geom_point(data = dflocalin061, aes(x = dflocalin061$long, y = dflocalin061$lat), color="orange", size=1, alpha=0.5)
x071=geom_point(data = dflocalin071, aes(x = dflocalin071$long, y = dflocalin071$lat), color="red", size=1, alpha=0.5)

#Now let us display locations of 3 chosen CAMEO events related to economy in India
map+x0231+x0331+x071
legend('topright',c("0231:Appeal for Economic Aid","0331:Intent for Economic Aid","071:Provide Economic Aid"),col=c("yellow","blue","red"),pch=16)
title("GDELT Event Locations in India:Economic Events in Years 2014-2015")

#------------------------USE CASE 2-------------------------
#Use Case 2 identifies Top 25 locations with highest density/frequency of events
gdy<-sql(hiveContext,"SELECT cn,							  
							 lat,
							 long,
							 year,
							 count(eventcode) as totevents		       
							 FROM gdt
							 GROUP BY cn,lat,long,year
							 ")

registerTempTable(gdy, "gdy")
cacheTable(hiveContext, "gdy")

#save results locally
dflocal2=collect(select(gdy,"*"))
save(dflocal2, file = "gdeltlocal2.Rdata")

#Determine top 25 locations with highest density of events in each country
#First let us filter by country
#We can use a different color for each year (i.e filter by year within each country and apply diff color)

#filter by US and IN
dflocalus2=subset(dflocal2,dflocal2$cn=='US')
dflocalin2=subset(dflocal2,dflocal2$cn=='IN')
#then by count of events
dfustop=head(arrange(dflocalus2,desc(totevents)),25)
dfintop=head(arrange(dflocalin2,desc(totevents)),25)
dfustop
dfintop

#let's plot on map for US
plot.new()
title("GDELT Event Locations in 2014-2015:Density of Economy based Events",font=3)
map=qmap('USA',zoom=3)

map + geom_point(data = dfustop, aes(x = dfustop$long, y = dfustop$lat), color="red", size=4, alpha=0.5)
title("Top 25 GDELT Economy related Event Locations in USA",font=2)

#lets plot on map for IN
map=qmap('India',zoom=4)

map + geom_point(data = dfintop, aes(x = dfintop$long, y = dfintop$lat), color="red", size=4, alpha=0.5)
title("Top 25 GDELT Economy related Event Locations in India",font=2)
dev.off()
#stop sparkR context
sparkR.stop()
ls()
###################Core Code Logic above###########################