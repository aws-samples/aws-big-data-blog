#!/usr/bin/ruby
require 'rubygems'
require 'json'
require 'rexml/document'
require 'net/http'
 
def run(cmd)
  puts "Running: #{cmd}"
  system("#{cmd}")
end
 
@target_dir = "/usr/share/elasticsearch/"
 
def getClusterMetaData
  metaData = {}
  jobFlow = JSON.parse(File.read('/mnt/var/lib/info/job-flow.json'))
  jobFlowExtra = JSON.parse(File.read('/mnt/var/lib/info/extraInstanceData.json'))
  userData = JSON.parse(Net::HTTP.get(URI('http://169.254.169.254/latest/user-data/')))
 
  #Determine if Instance Has IAM Roles
  req = Net::HTTP.get_response(URI('http://169.254.169.254/latest/meta-data/iam/security-credentials/'))
  metaData['roles'] = (req.code.to_i == 200) ? true : false
 
  metaData['instanceId'] = Net::HTTP.get(URI('http://169.254.169.254/latest/meta-data/instance-id/'))
  metaData['instanceType'] = Net::HTTP.get(URI('http://169.254.169.254/latest/meta-data/instance-type/'))
  metaData['ip'] = Net::HTTP.get(URI('http://169.254.169.254/latest/meta-data/local-ipv4/'))
  metaData['region'] = jobFlowExtra['region']
  metaData['masterPrivateDnsName'] = jobFlow['masterPrivateDnsName']
  metaData['cluster_Name'] = jobFlow['jobFlowId']
  metaData['isMaster'] = userData['isMaster']
 
  return metaData
end
 
def configure_elasticsearch()
  clusterMetaData = getClusterMetaData
  File.open("/etc/elasticsearch/elasticsearch.yml", "a+") do |config|
    config.puts("cluster.name: #{clusterMetaData['cluster_Name']}")
    config.puts("cloud.aws.region: #{clusterMetaData['region']}")
    config.puts("discovery.ec2.tag.aws:elasticmapreduce:job-flow-id: #{clusterMetaData['cluster_Name']}")
  end
end
 
def install_elasticsearch_aws_plugin(target_dir)
  run("#{target_dir}bin/plugin -install elasticsearch/elasticsearch-cloud-aws/2.6.0")
end
 
def install_elasticsearch_hdfs_repository_plugin(target_dir)
  run("#{target_dir}bin/plugin -install elasticsearch/elasticsearch-repository-hdfs/2.1.0-hadoop2")
end
 
def install_hadoop_plugin(target_dir)
  run("wget https://download.elasticsearch.org/hadoop/elasticsearch-hadoop-2.1.0.zip --no-check-certificate")
  run("mv elasticsearch-hadoop-2.1.0.zip #{target_dir}")
  run("unzip #{target_dir}elasticsearch-hadoop-2.1.0.zip -d #{target_dir}")
  run("echo export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:#{@target_dir}elasticsearch-hadoop-2.1.0/dist/* >> ~/.bashrc")
end
 
def clean_up
  run "rm -Rf #{@target_dir}elasticsearch-hadoop-2.1.0.zip"
end
 
configure_elasticsearch()
install_hadoop_plugin(@target_dir)
install_elasticsearch_aws_plugin(@target_dir)
install_elasticsearch_hdfs_repository_plugin(@target_dir)
 
run "service elasticsearch start"
 
clean_up
