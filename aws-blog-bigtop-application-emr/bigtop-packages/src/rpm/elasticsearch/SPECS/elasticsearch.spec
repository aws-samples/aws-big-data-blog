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
 
%define elasticsearch_name elasticsearch
%define elasticsearch_folder %{elasticsearch_name}-%{elasticsearch_base_version}
%define _binaries_in_noarch_packages_terminate_build   0
 
Name: elasticsearch
Version: %{elasticsearch_version}
Release: %{elasticsearch_release}
Summary: elasticsearch
License: 2013, Elasticsearch
Distribution: Elasticsearch
Group: Application/Internet
Packager: Elasticsearch
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{elasticsearch_name}-%{version}-%{release}-XXXXXX)
 
Source0: %{name}-%{version}.tar.gz
Source1: do-component-build
Source2: install_elasticsearch.sh
Source3: logging.yml
 
%global initd_dir %{_sysconfdir}/init.d
 
%if %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d
%else
# Required for init scripts
Requires: /lib/lsb/init-functions
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif
 
%description
Elasticsearch - Open Source, Distributed, RESTful Search Engine
 
%package doc
Summary: elasticsearch documentation
Group: Documentation
%description doc
Elasticsearch documentation
 
%prep
%setup -n %{elasticsearch_folder}
 
%build
bash $RPM_SOURCE_DIR/do-component-build
 
%install
rm -rf $RPM_BUILD_ROOT
# See /usr/lib/rpm/macros for info on how vars are defined.
bash %{SOURCE2} \
--build-dir=%{buildroot} \
--bin-dir=%{_bindir} \
--data-dir=%{_datadir} \
--libexec-dir=%{_libexecdir} \
--var-dir=%{_var} \
--prefix="${RPM_BUILD_ROOT}" \
--distro-dir=$RPM_SOURCE_DIR
 
# directory/file structure must first be created at install stage. In this cases, using elasticsearch-install.sh script.
%files
%defattr(644,root,root,755)
%config(noreplace) "/etc/elasticsearch/"
%config(noreplace) "/etc/sysconfig/"
%config %attr(755,root,root) "/usr/lib/systemd/system/"
%config %attr(755,root,root) "/usr/lib/sysctl.d/"
%config "/usr/lib/tmpfiles.d/"
%dir %attr(755,elasticsearch,elasticsearch) "/var/run/elasticsearch/"
%dir %attr(755,elasticsearch,elasticsearch) "/var/lib/elasticsearch/"
%dir %attr(755,elasticsearch,elasticsearch) "/usr/share/elasticsearch/logs"
%dir %attr(755,elasticsearch,elasticsearch) "/var/log/elasticsearch/"
%attr(755,root,root) /usr/share/elasticsearch/bin/
/usr/share/elasticsearch/lib/
/usr/share/elasticsearch/lib/sigar/
/usr/share/elasticsearch/bin/
/usr/share/elasticsearch/plugins
/etc/elasticsearch/
 
 
%pre
getent group elasticsearch >/dev/null || groupadd -r elasticsearch
getent passwd elasticsearch >/dev/null || \
useradd -r -g elasticsearch -d /usr/share/elasticsearch -s /sbin/nologin \
-c "elasticsearch user" elasticsearch
 
%post
 
%preun
 
%postun
