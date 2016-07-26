#!/bin/bash
set -x

echo "creating custom Bigtop repository"
sudo aws s3 cp s3://your-bucket/bigtop/repos/bigtop_custom.repo  /etc/yum.repos.d/bigtop_custom.repo
echo "creating application module and manifest directory"
sudo mkdir -p /home/hadoop/bigtop/bigtop-deploy/puppet/modules/elasticsearch/manifests/
echo "changing directory"
cd /home/hadoop/bigtop/bigtop-deploy/puppet/modules/elasticsearch/manifests/
echo "getting puppet recipe"
sudo wget https://raw.githubusercontent.com/awslabs/aws-big-data-blog/master/aws-blog-bigtop-application-emr/bigtop-deploy/puppet/modules/elasticsearch/manifests/init.pp
echo "creating application template directory"
sudo mkdir -p /home/hadoop/bigtop/bigtop-deploy/puppet/modules/elasticsearch/templates/
echo "changing directory"
cd /home/hadoop/bigtop/bigtop-deploy/puppet/modules/elasticsearch/templates/
echo "getting puppet templates"
sudo wget https://raw.githubusercontent.com/awslabs/aws-big-data-blog/master/aws-blog-bigtop-application-emr/bigtop-deploy/puppet/modules/elasticsearch/templates/elasticsearch.yml
sudo wget https://raw.githubusercontent.com/awslabs/aws-big-data-blog/master/aws-blog-bigtop-application-emr/bigtop-deploy/puppet/modules/elasticsearch/templates/elasticsearch.conf

echo "running puppet apply"
sudo puppet apply --verbose -d --modulepath=/home/hadoop/bigtop/bigtop-deploy/puppet/modules:/etc/puppet/modules  -e 'include elasticsearch::client'
