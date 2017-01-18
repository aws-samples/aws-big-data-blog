#!/bin/bash

#######   EDIT THESE - START  ########
JOBSERVER_VERSION="v0.6.2"
EMR_SH_FILE="s3://dgraeberaws-blogs/jobserver/jobserver_configs/emr_v1.6.1.sh"
EMR_CONF_FILE="s3://dgraeberaws-blogs/jobserver/jobserver_configs/emr_contexts_ml.conf"
S3_BUILD_ARCHIVE_DIR="s3://dgraeberaws-blogs/jobserver/dist"
#######   EDIT THESE - END  ########

if grep isMaster /mnt/var/lib/info/instance.json | grep true;
then
    #Prep work for the install
    mkdir /mnt/var/log/spark-jobserver  -p
    sudo mkdir /mnt/lib/spark-jobserver -p
    sudo chown hadoop:hadoop /mnt/lib/spark-jobserver
    mkdir -p /mnt/lib/.ivy2
    ln -s /mnt/lib/.ivy2 ~/.ivy2

    #Get the dependency applications
    curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
    sudo yum install sbt --nogpgcheck -y
    sudo yum install git -y

    #Clone the jobserver codebase
    sudo mkdir /mnt/work -p
    cd /mnt/work
    sudo git clone https://github.com/spark-jobserver/spark-jobserver.git
    cd spark-jobserver
    sudo git checkout $JOBSERVER_VERSION
    sudo sbt clean update package assembly

    # NOW is where you need to copy over the config/emr.sh and config/emr.conf
    sudo aws s3 cp $EMR_SH_FILE /mnt/work/spark-jobserver/config/emr.sh
    sudo aws s3 cp $EMR_CONF_FILE /mnt/work/spark-jobserver/config/emr.conf

    #Create the tar distro - just to be on the safe side
    sudo ./bin/server_package.sh emr

    #Copy it back to S3 in case you don't want to build it again
    sudo aws s3 cp /tmp/job-server/job-server.tar.gz $S3_BUILD_ARCHIVE_DIR/job-server-$JOBSERVER_VERSION.tar.gz

    #Stage and untar the distro
    cd /mnt/lib/spark-jobserver
    sudo tar zxf /tmp/job-server/job-server.tar.gz
    sudo chown hadoop:hadoop /mnt/lib/spark-jobserver -R
fi
exit 0




