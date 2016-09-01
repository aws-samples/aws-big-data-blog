#!/bin/bash -e

infodir="/mnt/var/lib/info"
pip="pip-2.7"
unset awscli boto mnts

VERSION="0.0.1"
SELF=`basename $0 .sh`
CURLOPTS="--connect-timeout 20 --retry 3 --retry-delay 5 -s -f"
AWS_METADATA="http://169.254.169.254/latest/meta-data"

attached_devices() {
    for i in $(curl $CURLOPTS $AWS_METADATA/block-device-mapping/); do
        case $i in
            "$1"*)
                curl $CURLOPTS $AWS_METADATA/block-device-mapping/$i | \
		        sed 's/^sd/xvd/'
                echo ;;
            *) : ;;
        esac
    done
}

print_version()
{
    echo $SELF version:$VERSION
}

print_usage() 
{
    cat << EOF

USAGE: ${0} -options 

OPTIONS                     DESCRIPTION                      
 
 OPTIONAL
 -a <awscli version>        AWS cli version for pip install
 -b <boto_version>          Boto version for pip install

 FLAGS
 -P                         Install deps for Python 2.6  
 -D                         Enable debug mode
 -V                         Print version
 -h                         Print usage
 
EOF
}

while getopts ":b:a:PDVh" optname; do
    case $optname in
        a)  awscli="==$OPTARG" ;;
        b)  boto="==$OPTARG" ;;
        P)  pip="pip2.6" ;;
        D)  set -x ;;
        h)  print_usage
            exit 0 ;;
        V)  print_version
            exit 0 ;;
        ?)  if [ "$optname" == ":" ]; then
                echo "Option ${OPTARG} requires a parameter" 1>&2
            else
                echo "Option ${OPTARG} unkown" 1>&2
            fi
            exit 1;;
    esac
done

# Install mapper and reducer dependencies 
sudo yum update -y python-argparse openssl

yes | sudo $pip install boto${boto} --upgrade
yes | sudo $pip install awscli${awscli} --upgrade

## Find mounted ephemeral drives
for i in `attached_devices "ephemeral"`; do
    mp=$(awk -v d="/dev/$i" '{ if($1 == d && $2 ~ "/mnt") print $2 }' \
            /etc/mtab)
    if [ -z "$mp" ]; then 
        continue
    fi

    if [ -z "$mnts" ]; then
        mnts="ephemeral=$mp"
    else
        mnts+=",$mp"
    fi
    
    ## Add hadoop group w permissions 
    chmod 775 $mp
done

## Set default boto options
cat <<EOF | sudo tee /etc/boto.cfg 1>/dev/null
[Boto]
debug = 0
num_retries = 10

[s3]
calling_format = boto.s3.connection.OrdinaryCallingFormat
EOF

## Set default campanile config 
cat <<EOF | sudo tee /etc/campanile.cfg 1>/dev/null 
[DEFAULT]
maxtmpsize=134217728
$mnts

[loggers]
keys=root,verbose

[handlers]
keys=verboseHandler,syslogHandler

[formatters]
keys=verbose,syslog

[logger_root]
level=INFO
handlers=syslogHandler

[logger_verbose]
level=INFO
handlers=verboseHandler,syslogHandler
qualname=verbose
propagate=0

[handler_verboseHandler]
class=StreamHandler
formatter=verbose
args=(sys.stdout,)

[handler_syslogHandler]
class=handlers.SysLogHandler
formatter=syslog
args=("/dev/log",handlers.SysLogHandler.LOG_LOCAL0)

[formatter_verbose]
format=%(asctime)s %(module)s[%(process)d]: %(message)s
datefmt=%b %m %H:%M:%S

[formatter_syslog]
format=%(module)s[%(process)d]: %(message)s
EOF
