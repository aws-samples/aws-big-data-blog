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
    package { ["elasticsearch"]: ensure => latest, }

    file {
        "/usr/share/elasticsearch/bin/elasticsearch_configure.rb":
        content => template("elasticsearch/elasticsearch_configure.rb"),
        mode  => 755,
        require => [Package["elasticsearch"]],
    }

    exec { "configure elasticsearch":
        command => "/usr/share/elasticsearch/bin/elasticsearch_configure.rb",
        require => [Package["elasticsearch"]],
    }
  }

}
