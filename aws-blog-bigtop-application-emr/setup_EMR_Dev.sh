#!/bin/bash

# @author  : vivanih@
# @created : 2016-06-20
# @updated : -
# CHANGELOG :
#
# TODO :
#

set -x

echo "install dev packages"
sudo yum -y install wget tar git subversion gcc gcc-c++ make cmake ant fuse autoconf automake libtool sharutils xmlto lzo-devel zlib-devel fuse-devel openssl-devel python-devel libxml2-devel libxslt-devel cyrus-sasl-devel sqlite-devel mysql-devel openldap-devel rpm-build createrepo redhat-rpm-config

echo "installing Gradle"
cd /opt/
sudo wget https://services.gradle.org/distributions/gradle-2.13-bin.zip
sudo unzip gradle-2.13-bin.zip
sudo ln -s gradle-2.13 gradle
echo -e "\nexport PATH=/opt/gradle/bin:$PATH" >> ~/.bashrc

echo "installing Maven"
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
