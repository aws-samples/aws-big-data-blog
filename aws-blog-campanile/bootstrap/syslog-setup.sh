#!/bin/bash -e

infodir="/mnt/var/lib/info"
shutdown="/mnt/var/lib/instance-controller/public/shutdown-actions"
facility="local0"
logfile="/dev/shm/emr.log"
protocol="@@" ## TCP

VERSION="0.0.2"
SELF=`basename $0 .sh`

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
 -s <s3path>                S3 path for logfile upload. ie. <bucket>/logs
 -f <facility>              Facility for EMR syslog traffic. Default:$facility 
 -l <logfile>               EMR logfile. Default:$logfile

 FLAGS
 -D                         Enable debug mode
 -V                         Print version
 -h                         Print usage
 -u                         Enable udp mode
 
EOF
}

while getopts ":f:s:l:DVhu" optname; do
    case $optname in
        D)  set -x ;;
        s)  s3="$OPTARG" ;;
        f)  facility="$OPTARG" ;;
	l)  logfile="$OPTARG" ;;
        u)  protocol="@" ;;
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

if grep '"isMaster": true' $infodir/instance.json > /dev/null; then 
    ## Verify logfile is accessible   
    sudo touch $logfile
   
    ## Install deps
    sudo yum -y install aws-cli rsyslog logrotate
    
    ## Syslogd config
    cat <<EOF | sudo tee /etc/rsyslog.d/master.conf 1>/dev/null
\$ModLoad imudp
\$UDPServerRun 514
\$ModLoad imtcp
\$InputTCPServerRun 514
\$SystemLogRateLimitInterval 0
${facility}.*$(echo -en "\t\t\t\t\t")-${logfile}
EOF

    ## LogRotate config
    logname=`basename ${logfile} .log`
    cat <<EOF | sudo tee /etc/logrotate.d/$logname 1>/dev/null
${logfile}
{
    size 1024M
    rotate 10
    postrotate
    /bin/kill -HUP \`cat /var/run/syslogd.pid 2>/dev/null\` 2>/dev/null || true
    endscript
    compress
    dateext
    dateformat .%s
EOF
    
    ## Set backup to log-uri if available and location not set
    if [ -z "$s3" ]; then
        eval $(grep -A2 "  type: USER_LOG" $infodir/job-flow-state.txt | tr -d ' ' | tr ':' '=' | tr '"' \')
        if [ -n "$bucket" ]; then
            s3="$bucket/${keyPrefix}syslog/"
        else
            : ## S3 backup not enabled          
        fi
    else
        jobid=`awk -F':' '/jobFlowId/ { 
                gsub(/(^ *|"|,)/,"",$2)
                print $2 
                }' $infodir/job-flow.json`
        s3="$s3/$jobid/syslog/"
    fi
   
    if [ -n "$s3" ]; then 
        ## Enable backup to s3
        eval `echo ${s3#*//} | sed -e 's@^\([^/]*\).*@bucket=\1@'`
        region=`aws s3api get-bucket-location --bucket $bucket --query \
					                'LocationConstraint'`
        : ${region:=us-east-1}

        ## Location returns null in newer version of cli
        if [ "$region" == "null" ]; then
            region='us-east-1'
        fi
        
        ## Add upload to S3 script 
        cat <<EOF | sudo tee -a /etc/logrotate.d/$logname 1>/dev/null
    lastaction
        for i in \${1//[[:space:]]}.*.gz; do
	    aws s3 mv "\$i" "s3://$s3" --region ${region//\"/} --sse
        done
    endscript
EOF
        ## Force logrotate on term 
        cat <<EOF | sudo tee $shutdown/logrotate-$logname 1>/dev/null
#!/bin/bash
sudo /usr/sbin/logrotate -f /etc/logrotate.d/$logname
EOF
        sudo chmod +x $shutdown/logrotate-$logname

    fi
   
    ## Closing brace 
    cat <<EOF | sudo tee -a /etc/logrotate.d/$logname 1>/dev/null
}
EOF

    ## Run more frequent 
    sudo cat <<EOF | sudo tee /etc/cron.d/$logname 1>/dev/null
*/5 * * * * root /usr/sbin/logrotate /etc/logrotate.d/$logname
EOF

else
    ## Install deps
    sudo yum -y install rsyslog

    master=`awk -F':' '/masterPrivateDnsName/ { 
        gsub(/(^ *|"|,)/,"",$2)
        print $2 
    }' $infodir/job-flow.json`

    ## Create syslog fwd rule
   cat <<EOF | sudo tee /etc/rsyslog.d/fwd-master.conf 1>/dev/null
\$SystemLogRateLimitInterval 0
# ### begin forwarding rule ###
\$WorkDirectory /var/lib/rsyslog
\$ActionQueueFileName fwdRule1
\$ActionQueueMaxDiskSpace 2g
\$ActionQueueSaveOnShutdown on
\$ActionQueueType LinkedList
\$ActionResumeRetryCount -1
${facility}.* ${protocol}${master}:514
# ### end of the forwarding rule ###
EOF

fi
    
## Remove facility from standard /var/log/messages 
sudo sed -i".org" \
    -e '/messages$/s@^\([^ ]*\)\(.*\)@\1;'${facility}'.none\2@' \
    /etc/rsyslog.conf

sudo service rsyslog restart
