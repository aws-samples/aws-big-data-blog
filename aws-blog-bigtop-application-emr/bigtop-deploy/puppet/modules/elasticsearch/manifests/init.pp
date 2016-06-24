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

class elasticsearch {
  class client {

    $clusterId = generate ("/bin/bash", "-c", "cat /mnt/var/lib/instance-controller/extraInstanceData.json |grep \"jobFlowId\" | cut -d':' -f2 | tr -d '\"' |tr -d ',' |tr -d ' '")
    $region = generate ("/bin/bash", "-c", "cat /mnt/var/lib/instance-controller/extraInstanceData.json |grep 'region\"' | cut -d':' -f2 | tr -d '\"' |tr -d ',' |tr -d ' '")
    $isMaster = generate ("/bin/bash", "-c", "cat /mnt/var/lib/info/instance.json | grep \"isMaster\" | cut -d':' -f2 | tr -d '\"' |tr -d ',' |tr -d ' '")
    $elasticsearch_port = '9200'

    include common

  }


  class common () {
    package { ["elasticsearch"]: ensure => latest, }

    file {
      "/etc/elasticsearch/elasticsearch.yml":
      content => template("elasticsearch/elasticsearch.yml"),
#      require => [Package["elasticsearch"]],
#      notify => Service['elasticsearch'],
    }

    exec { "install aws plugin":
      command => "/usr/share/elasticsearch/bin/plugin -install elasticsearch/elasticsearch-cloud-aws/2.6.0",
      require => [Package["elasticsearch"]],
    }
    
    exec { "start elasticsearch":
      command => "/etc/init.d/elasticsearch start",
      subscribe => [File["/etc/elasticsearch/elasticsearch.yml"]],
    }
#    service { "elasticsearch":
#      ensure =>running,
#      require => [Package["elasticsearch"], File["/etc/elasticsearch/elasticsearch.yml"]],
#      subscribe => [File["/etc/elasticsearch/elasticsearch.yml"]],
#      hasrestart => true,
#      hasstatus => true,
#    }

  }
}
