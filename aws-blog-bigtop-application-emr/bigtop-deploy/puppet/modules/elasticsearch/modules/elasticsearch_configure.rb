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
  jobFlowExtra = JSON.parse(File.read('/mnt/var/lib/instance-controller/extraInstanceData.json'))
  userData = JSON.parse(Net::HTTP.get(URI('http://169.254.169.254/latest/user-data/')))

  metaData['region'] = jobFlowExtra['region']
  metaData['cluster_Name'] = jobFlow['jobFlowId']
  metaData['isMaster'] = userData['isMaster']

  return metaData
end

def configure_elasticsearch()
  clusterMetaData = getClusterMetaData
  File.open("/etc/elasticsearch/elasticsearch.yml", "a+") do |config|
    if clusterMetaData['isMaster']==true
       config.puts("http.port: 9200")
    else
       config.puts("http.port: 9202")
    end
    config.puts("node.master: #{clusterMetaData['isMaster']}")
    config.puts("node.data: true")
    config.puts("cluster.name: #{clusterMetaData['cluster_Name']}")
    config.puts("cloud.aws.region: #{clusterMetaData['region']}")
    config.puts("discovery.ec2.tag.aws:elasticmapreduce:job-flow-id: #{clusterMetaData['cluster_Name']}")
  end
end

def install_elasticsearch_aws_plugin(target_dir)
  run("#{target_dir}bin/plugin -install elasticsearch/elasticsearch-cloud-aws/2.6.0")
end

configure_elasticsearch()
install_elasticsearch_aws_plugin(@target_dir)

run "service elasticsearch start"
