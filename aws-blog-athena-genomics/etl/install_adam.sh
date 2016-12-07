#!/bin/bash

sudo yum -y install git

# Install Maven
wget http://apache.claz.org/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar xvzf apache-maven-3.3.9-bin.tar.gz
export PATH=/home/hadoop/apache-maven-3.3.9/bin:$PATH

# Install ADAM
git clone https://github.com/bigdatagenomics/adam.git
cd adam
sed -i 's/2.6.0/2.7.2/' pom.xml
./scripts/move_to_spark_2.sh
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
mvn clean package -DskipTests

export PATH=/home/hadoop/adam/bin:$PATH

cd ~/