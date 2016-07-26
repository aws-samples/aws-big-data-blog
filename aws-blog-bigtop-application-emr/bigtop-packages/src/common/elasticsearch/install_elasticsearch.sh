#!/bin/bash
 
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
 
set -ex
 
usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --distro-dir=DIR path to distro specific files (debian/RPM)
     --build-dir=DIR             path to Tachyon dist.dir
     --prefix=PREFIX             path to install into
 
  Optional options:
     --bin-dir=DIR               path to install bin
     --data-dir=DIR              path to install local Tachyon data
     ... [ see source for more similar options ]
  "
  exit 1
}
 
OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'distro-dir:' \
  -l 'bin-dir:' \
  -l 'libexec-dir:' \
  -l 'var-dir:' \
  -l 'lib-dir:' \
  -l 'data-dir:' \
  -l 'build-dir:' -- "$@")
 
if [ $? != 0 ] ; then
    usage
fi
 
eval set -- "$OPTS"
while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --libexec-dir)
        LIBEXEC_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --var-dir)
        VAR_DIR=$2 ; shift 2
        ;;
        --data-dir)
        DATA_DIR=$2 ; shift 2
        ;;
        --)
        shift ; break
        ;;
        *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
done
 
for var in PREFIX BUILD_DIR DISTRO_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done
 
LIB_DIR=${LIB_DIR:-/usr/share/elasticsearch}
LIBEXEC_DIR=${INSTALLED_LIB_DIR:-/usr/libexec}
BIN_DIR=${BIN_DIR:-/usr/bin}
 
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/plugins
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/libexec
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/lib/sigar
install -d -m 0755 $PREFIX/$LIB_DIR/logs
install -d -m 0755 $PREFIX/$DATA_DIR
install -d -m 0755 $PREFIX/$DATA_DIR/elasticsearch
install -d -m 0755 $PREFIX/etc
install -d -m 0755 $PREFIX/etc/elasticsearch
install -d -m 0755 $PREFIX/etc/sysconfig
install -d -m 0755 $PREFIX/etc/sysconfig/elasticsearch
install -d -m 0755 $PREFIX/etc/init.d
install -d -m 0755 $PREFIX/usr/lib/systemd/system
install -d -m 0755 $PREFIX/usr/lib/sysctl.d
install -d -m 0755 $PREFIX/usr/lib/tmpfiles.d
install -d -m 0755 $PREFIX/$VAR_DIR/log/elasticsearch
install -d -m 0755 $PREFIX/var/run/elasticsearch
install -d -m 0755 $PREFIX/var/lib/elasticsearch
install -d -m 0755 $PREFIX/var/log/elasticsearch
 
#cp -ra ${BUILD_DIR}/dist/lib/*.*ar $PREFIX/${LIB_DIR}/lib/
#echo "==============================TARGET files============================="
#ls -l target/
cp -a lib/sigar/libsigar*.so $PREFIX/${LIB_DIR}/lib/sigar/
cp -a target/lib/a*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/elasticsearch*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/lib/groovy*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/lib/j*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/lib/log*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/lib/lucene*.jar $PREFIX/${LIB_DIR}/lib/
cp -a target/lib/spatial*.jar $PREFIX/${LIB_DIR}/lib/
 
cp -a target/bin/elasticsearch $PREFIX/${LIB_DIR}/bin/
cp -a target/bin/elasticsearch.in.sh $PREFIX/${LIB_DIR}/bin/
cp -a target/bin/plugin $PREFIX/${LIB_DIR}/bin/
 
cp -a $DISTRO_DIR/logging.yml $PREFIX/etc/elasticsearch
 
 
