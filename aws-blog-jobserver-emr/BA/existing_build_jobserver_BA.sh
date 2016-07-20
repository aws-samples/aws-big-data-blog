#!/bin/bash


S3_FULL_PATH_TAR="s3://dgraeberaws-blogs/jobserver/dist/job-server-v0.6.2.tar.gz"
EMR_SH_FILE="s3://dgraeberaws-blogs/jobserver/jobserver_configs/emr_v1.6.1.sh"
EMR_CONF_FILE="s3://dgraeberaws-blogs/jobserver/jobserver_configs/emr_contexts.conf"

if grep isMaster /mnt/var/lib/info/instance.json | grep true;
then
    mkdir /mnt/var/log/spark-jobserver  -p
    sudo mkdir /mnt/lib/spark-jobserver -p

    sudo chown hadoop:hadoop /mnt/lib/spark-jobserver

    cd /mnt/lib/spark-jobserver
    aws s3 cp $S3_FULL_PATH_TAR job-server.tar.gz

    tar zxf job-server.tar.gz
    sudo aws s3 cp $EMR_SH_FILE settings.sh
    sudo aws s3 cp $EMR_CONF_FILE emr.conf

    sudo chown hadoop:hadoop /mnt/lib/spark-jobserver -R
fi

exit 0

#Start the server
#./server_start.sh



