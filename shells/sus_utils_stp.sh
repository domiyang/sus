#!/bin/sh
#
#- Description: 	This is the socket utils scripts help to support the socket utils functions in unix like env with stp mode on.
#- Date:			14-Jan-2017
#- Author: 		Domi Yang (domi_yang@hotmail.com)
#---------------------------------------------------------------------------
#   Copyright 2016 Domi Yang (domi_yang@hotmail.com)
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#---------------------------------------------------------------------------
#

#the usage
the_usage="usage: `basename $0`[find_sus_pid sus_port|kill_sus sus_port|start_sus sus_user sus_port|restart_sus sus_user sus_port|cmd_client ip_port sus_user cmd|ft_client ip_port sus_user file_to_upload path_on_remote_server|upgrade_sus sus_host sus_user sus_port|upgrade_sus_all sus_user sus_port]|cmd_client_all sus_user sus_port cmd|ft_client_all sus_user sus_port file_to_upload path_on_remote_server|sus_clean_up sus_user|deploy_sus sus_user sus_port"

#read from exported control flag for read_user_confirmation_flag (y/n, default as y)
export read_user_confirmation_flag=n

#enable the trace
set -x

#common configurations
java_executable_path=java

#the remote sus root path
sus_root=/Users/domi/sus

#the common file name
sus_utils_sh_file_name=sus_utils.sh

#the local sus root path, the jar, sh copy from when doing upgrade
sus_root_local=${sus_root}/deployment

#log file
#run_log_file=`basename $0`_$(date +%Y-%m-%d_%H%M%S).log
#log dir
run_log_dir=${sus_root}/`whoami`/logs
#check if need to create the dir: $run_log_dir
if [ ! -d "$run_log_dir" ] ; then
	mkdir -p $run_log_dir
	echo "done mkdir for run_log_dir=$run_log_dir"
fi

run_log_file=${run_log_dir}/`basename $0`_$(date +%Y-%m-%d_%H%M%S).log

#run the sus_utils
sh ${sus_root}/${sus_utils_sh_file_name} "$@"

#unset the export
unset read_user_confirmation_flag

#reset the trace
set +x

