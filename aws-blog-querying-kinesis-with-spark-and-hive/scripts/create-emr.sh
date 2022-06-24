#!/bin/bash
function usage_and_exit {
echo "usage $0 [release label (default: emr-4.5.0)] [key name] [instance type (default: m3.xlarge)]"
	exit 1
}

if [ "$#" -lt 1 ]; then
	usage_and_exit
fi

RELEASE_LABEL=emr-4.5.0
APPLICATIONS="Name=Spark Name=Hive"
KEY_NAME=
INSTANCE_TYPE=m3.xlarge

if [ "$#" -eq 1 ]; then
	KEY_NAME=$1
elif [ "$#" -ne 3]; then
	usage_and_exit
else 
	RELEASE_LABEL=$1
	KEY_NAME=$2
	INSTANCE_TYPE=$3
fi

INSTANCE_GROUPS="InstanceGroupType=MASTER,InstanceCount=1,BidPrice=0.08,InstanceType=$INSTANCE_TYPE InstanceGroupType=CORE,InstanceCount=2,BidPrice=0.08,InstanceType=$INSTANCE_TYPE"
BOOTSTRAP_ACTIONS="Path=s3://aws-bigdata-blog/artifacts/Querying_Amazon_Kinesis/DownloadKCLtoEMR400.sh,Name=InstallKCLLibs"

aws emr create-cluster --release-label $RELEASE_LABEL --applications $APPLICATIONS --ec2-attributes KeyName=$KEY_NAME --use-default-roles --instance-groups $INSTANCE_GROUPS --bootstrap-actions $BOOTSTRAP_ACTIONS --configurations https://s3-ap-southeast-1.amazonaws.com/helix-public/spark-defaults.json

