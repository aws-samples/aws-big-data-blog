#!/bin/bash

is_aml=`uname -r | grep amzn1.x86_64 | wc -l`

if [ is_aml=1 ]; then
	sudo yum -y install nodejs npm --enablerepo=epel
else
	echo "Unsupported OS"
	exit -1
fi