#!/bin/bash -e

infodir="/mnt/var/lib/info"
shutdown="/mnt/var/lib/instance-controller/public/shutdown-actions"
facility="LOG_LOCAL0"
loglevel="info"

VERSION="0.0.1"
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
 -l <loglevel>              See docs.saltstack.com for valid levels. Default:
                            info
 -f <facility>              See man syslog.h for valid facilities. Default:
                            LOG_LOCAL0

 FLAGS
 -D                         Enable debug mode
 -V                         Print version
 -h                         Print usage
 
EOF
}

while getopts ":f:l:DVh" optname; do
    case $optname in
        D)  set -x ;;
        f)  facility="$OPTARG" ;;
	l)  loglevel="$OPTARG" ;;
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
    ## Install deps 
    sudo yum --enablerepo=epel -y install salt-master
    
    ## Create conf and shared dirs
    sudo mkdir -p -m750 /etc/salt/master.d /srv/salt

    ## Conf file 
    cat <<EOF | sudo tee /etc/salt/master.d/aws.conf 1>/dev/null
log_level: $loglevel
log_file: udp://localhost:514/$facility
auto_accept : True
file_recv: True
file_roots:
  base:
    - /srv/salt
EOF
    
    ## Start service
    sudo service salt-master start
else 
    ## Get master hostname 
    master=`awk -F':' '/masterPrivateDnsName/ { 
        gsub(/(^ *|"|,)/,"",$2)
        print $2 
    }' $infodir/job-flow.json`

    ## Install deps 
    sudo yum --enablerepo=epel -y install salt-minion

    ## Create conf dirs
    sudo mkdir -p -m750 /etc/salt/minion.d
    
    ## Conf file 
    cat <<EOF | sudo tee /etc/salt/minion.d/aws.conf 1>/dev/null
log_level: $loglevel
log_file: udp://localhost:514/$facility
open_mode: True
master: $master
EOF
    
    ## Deregister on terminate 
    cat <<"EOF" | sudo tee /etc/init.d/salt-revoke 1>/dev/null
#!/bin/bash

### BEGIN INIT INFO
# Provides: saltutil.revoke_auth
# Required-Start: salt-minion
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Description: Revokes minion auth key on shutdown
### END INIT INFO

# Source function library
. /etc/rc.d/init.d/functions

# Default Parameters
RETVAL=0
PROG="salt-revoke"
LOCKFILE="/var/lock/subsys/$PROG"

start() {
    echo -n "Enable $PROG"
    if touch $LOCKFILE > /dev/null 2>&1; then 
        success
    else 
        RETVAL=1
        failure
    fi
    echo
}

stop() {
    echo -n "Revoking minion auth key"
    if salt-call saltutil.revoke_auth > /dev/null 2>&1; then
        rm -f $LOCKFILE > /dev/null 2>&1
        success
    else 
        RETVAL=1
        failure
    fi
    echo
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    *)
        echo $"Usage: $0 {start|stop}"
        exit 2
esac

exit $RETVAL
EOF
    sudo chmod +x /etc/init.d/salt-revoke
    sudo chkconfig --add salt-revoke

    ## Start services
    sudo service salt-minion start
    sudo service salt-revoke start
fi
