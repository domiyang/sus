#!/bin/sh
#
#- Description: 	This is the socket utils scripts help to support the socket utils functions in unix like env with stp mode support.
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
#export read_user_confirmation_flag=n

#enable the trace
set -x

#common configurations
java_executable_path=java

#the remote sus root path
sus_root=/Users/domi/sus

#the common file name
sus_jar_file_name=sus.jar
sus_utils_sh_file_name=sus_utils.sh
sus_utils_st_sh_file_name=sus_utils_stp.sh

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

#the local sus root path, the jar, sh copy from when doing upgrade
sus_root_local=${sus_root}/deployment

#list of servers to upgrade for sus if required
host_list="1.1.1.2 1.1.1.3 1.1.1.4 1.1.1.5 1.1.1.6"

#shared global variables
sus_pid=""

#some helper utils
#common log method with common prefixing. $1 - the prefix, $2 - the original calling method name $3 - the original message.
log_common(){
	local current_date_time
	local my_prefix
	local my_method_name
	local my_message
	current_date_time=$(date +%Y-%m-%d_%H:%M:%S)
	
	my_prefix=$1
	my_method_name=$2
	shift
	shift
	my_message="$@"
	echo "${current_date_time}|$my_prefix|$my_method_name|$my_message" >> $run_log_file
}

#log the error message $1 - the calling method name, $2 the msg
log_error(){
	local my_method_name
	local my_message

	my_method_name=$1
	
	#log_common "ERROR" "$1" "$2"
	if [ $# -ge 2 ] ; then
		shift
		my_message="$@"
        log_common "ERROR" "$my_method_name" "$my_message"
	else
        log_common "ERROR" "" "$my_method_name"
	fi
}

#log the info message $1 - the calling method name, $2 the msg
log_info(){
	local my_method_name
	local my_message

	my_method_name=$1
	
	#log_common "INFO" "$1" "$2"
	if [ $# -ge 2 ] ; then
		shift
		my_message="$@"
        log_common "INFO" "$my_method_name" "$my_message"
	else
        log_common "INFO" "" "$my_method_name"
	fi
}

#check if the user mathced with the whoami value, $1 - the user to check, exit 2 when it's not matched
is_user_allowed(){
	local method_name
	local current_user

	method_name="is_user_allowed"
	current_user=$(whoami)
	if [ "$1" != "$current_user" ] ; then
		log_error $method_name "the sus_user: $1 not matched with current user: $current_user, will not process"
	    exit_code $method_name 2
    fi

}

#log error and exit with code, $1 - the calling method name $2 - the exit code
exit_code(){
	local method_name

	method_name="exit_code"
	is_args_count_valid $# 2 "2 arguments expected: method_name exit_code"

	log_error $method_name "exit in method: $1 with code: $2"
	exit $2
}

#log info and return the value, $1 - the calling method name $2 - the value to return
return_value(){
	local method_name

	method_name="return_value"
	is_args_count_valid $# 2 "arguments expected: method_name value_to_return"

	log_info $method_name "return in method: $1 with value: $2"
	return $2
}

#check if arguments present and expected count matched. $1 - the count present, $2 - expected count  and $3 - the erorr msg.
is_args_count_valid(){
	local method_name

	method_name="is_args_count_valid"
	log_info $method_name "start"
	
	if [ $1 -ne $2 ] ; then
		log_error $method_name "$2 $3"
		exit_code $method_name 1
	fi
	
	log_info $method_name "end"
}

#check if arguments present has equals or more than expected count. $1 - the count present, $2 - expected count  and $3 - the erorr msg.
is_args_count_valid_ge(){
	local method_name

	method_name="is_args_count_valid_ge"
	log_info $method_name "start"
	
	if [ $1 -lt $2 ] ; then
		log_error $method_name "$2 $3"
		exit_code $method_name 1
	fi
	
	log_info $method_name "end"
}

#convert the path if it's running in CYGWIN mode on windows, $1 - the original path, return by echoing the original/converted path if required.
get_cygpath(){
	local method_name

	method_name="get_cygpath"
	#log_info $method_name "start"

	if [ "$(uname -s)" == *"CYGWIN"* ] ; then
		echo $(cygpath -w "$1")
	else
		echo $1	
	fi

	#log_info $method_name "end"
}

#get the user's confirmation on some dangerous operation, $1 - the prompting message, only user input y will be continue, exit 2 otherwise
get_user_confirmation(){
	local method_name

	method_name="get_user_confirmation"
	echo "$1"
	
	#handle for stp mode
	if [ "n" == "$read_user_confirmation_flag" ] ; then
		user_response=y
		log_info $method_name "default as y for user confirmation in stp mode"
	else
		read user_response
	fi
	
	if [ "y" != "$user_response" ] ; then
		log_info $method_name "existing the current process."
	    exit_code $method_name 2  
	fi
}

#kill the process for ${sus_jar_file_name} for the port (port of the process is unique), $1 - the sus_port, exit 0 when success.
kill_sus() {
	local method_name
	local sus_port

	method_name="kill_sus"
	log_info $method_name "start"
	is_args_count_valid $# 1 "arguments expected: sus_port"

	sus_port=$1
	#call to set the sus_pid
	find_sus_pid $1

	log_info $method_name "original sus_pid=$sus_pid for port: $sus_port"
	if [[ "" == "$sus_pid" ]] ; then
		log_info $method_name "not process id found: $sus_pid"
		exit_code $method_name 2
	fi

	log_info $method_name "sus_pid=$sus_pid"		
	log_info $method_name "ps before kill sus_pid=$sus_pid"
	ps -ef | grep "$sus_pid"
	log_info $method_name "start killing sus_pid=$sus_pid"
	kill -9 "$sus_pid"
	log_info $method_name "ps after kill sus_pid=$sus_pid"
	ps -ef | grep "$sus_pid"

	#resetting the global shared variables
	sus_pid=""	
	log_info $method_name "reset sus_pid=$sus_pid"

	log_info $method_name "end"

	return_value $method_name 0
	
}

#find the process id for the ${sus_jar_file_name} for the port, $1 - the sus_port, return 0 and set the sus_pid shared variable, return 2 otherwise.
find_sus_pid() {
	local method_name

	method_name="find_sus_pid"

	log_info $method_name "start"

	is_args_count_valid $# 1 "arguments expected: sus_port"

	sus_port=$1
	log_info $method_name "finding sus_pid for port: $sus_port"
	ps -ef | grep "${sus_jar_file_name}" | grep "$sus_port"
	#setting the global shared variables
	sus_pid=`ps -ef | grep "${sus_jar_file_name}" | grep "$sus_port" | awk -F " " '{print $2}'`
	if [ "" == "$sus_pid" ] ; then
		log_error $method_name "not found sus_pid for port: $sus_port"
		return_value $method_name 2
	else
		log_info "found sus_pid=$sus_pid for port: $sus_port"
		return_value $method_name 1
	fi
	
	#log_info $method_name "end"
}

#start the sus server in the background, $1 - the system user to run with (logger/dmbdp1/wasuser), $2 - the port sus_server to listening on (1983,1984,1985), return 0 when started successfully, 2 otherwise.
start_sus() {
	local method_name
	local sus_user
	local sus_port
	local sus_jar_path
	local sus_skey_file
	local sus_dir
	local sus_log_file
	local sus_jar_path

	method_name="start_sus"
	log_info $method_name "start"
	
	is_args_count_valid $# 2 "arguments expected: sus_user sus_port"

	sus_user=$1
	sus_port=$2

	log_info $method_name "going to start sus with user: $sus_user on port: $sus_port"

	is_user_allowed $sus_user

	#configure accordingly base on the $sus_user and $sus_port
	sus_jar_path=${sus_root}/${sus_user}/${sus_jar_file_name}
	sus_skey_file=${sus_root}/${sus_user}/skey_file_${sus_user}.txt
	sus_skey_file=$(get_cygpath $sus_skey_file)
	sus_dir=${sus_root}/${sus_user}/out_dump/server_${sus_port}
	sus_log_file=${sus_root}/${sus_user}/server_${sus_user}_${sus_port}.log
	
	sus_jar_path=$(get_cygpath $sus_jar_path)
	
	#check if jar file exist
	if [ ! -f "${sus_jar_path}" ]; then
		log_error $method_name "${sus_jar_path} does not exist, not able to proceed!"
		exit_code $method_name 2 
	fi
	
	log_info $method_name "after sus_jar_path=$sus_jar_path"
	log_info $method_name "ps before start sus for sus_port: $sus_port"
	ps -ef | grep "${sus_jar_file_name}" | grep "$sus_port"
	
	log_info $method_name "going to start sus server with user: $sus_user on port: $sus_port"
	nohup ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} server ${sus_port} ${sus_dir} >> ${sus_log_file} 2>&1 &

	log_info $method_name "ps after start sus for sus_port: $sus_port"
	ps -ef | grep "${sus_jar_file_name}" | grep "$sus_port"
	
	return_value $method_name 0
	
	#log_info $method_name "end"
}

#restart the sus server, $ - sus_user, $2 - sus_port, return 0 when success, 2 otherwise
restart_sus() {
	local method_name
	local sus_user
	local sus_port

	method_name="restart_sus"
	log_info $method_name "start"

	is_args_count_valid $# 2 "arguments expected: sus_user sus_port"

	sus_user=$1
	sus_port=$2

	log_info $method_name "going to kill the sus server for user: $sus_user on port: $sus_port"
	is_user_allowed $sus_user
		
	kill_sus $sus_port
	
	ret_val=$?
	log_info $method_name "ret_val=$ret_val"

	if [ $ret_val -ne 0 ] ; then
		exit_code $method_name $ret_val
	fi

	start_sus $sus_user $sus_port
	ret_val=$?
	if [ $ret_val -ne 0 ] ; then
		exit_code $method_name $ret_val
	fi

	return_value $method_name 0

	#log_info $method_name "end"
}

##use to upgrade single server, $1 - the sus server ip, $2 - the user own this sus server, $3 - the port server listening, return 0 success. 
upgrade_sus() {
	local method_name
	local sus_host
	local sus_user
	local sus_port
	local sus_jar_path
	local sus_utils_sh_path
	local sus_utils_stp_sh_path
	local sus_skey_file
	local sus_root_user_dir

	method_name="upgrade_sus"
	log_info $method_name "start"
	
	is_args_count_valid $# 3 "arguments expected: sus_host sus_user sus_port"

	sus_host=$1
	sus_user=$2
	sus_port=$3

	#configure accordingly to local env
    sus_jar_path="${sus_root_local}/${sus_jar_file_name}"
	sus_jar_path=$(get_cygpath $sus_jar_path)
	sus_utils_sh_path="${sus_root_local}/${sus_utils_sh_file_name}"
	sus_utils_sh_path=$(get_cygpath $sus_utils_sh_path)
	sus_utils_stp_sh_path="${sus_root_local}/${sus_utils_st_sh_file_name}"
	sus_utils_stp_sh_path=$(get_cygpath $sus_utils_stp_sh_path)
	sus_skey_file=${sus_root}/${sus_user}/skey_file_${sus_user}.txt
	sus_skey_file=$(get_cygpath $sus_skey_file)
	
	log_info "going to upgrade sus with host: $sus_host user: $sus_user on port: $sus_port sus_jar: $sus_jar_path sus_utils_sh_path: $sus_utils_sh_path sus_skey_file: $sus_skey_file"

	#confirm to continue
	get_user_confirmation "are you sure want to upgrade the hosts: $sus_host for user: $sus_user on port: $sus_port? y/n"

	#the server sus installation path
	sus_root_user_dir=${sus_root}/${sus_user}

	log_info $method_name "uploading file: $sus_jar_path to path on server: $sus_root_user_dir"
    ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ft-client ${sus_host}#${sus_port} ${sus_jar_path} ${sus_root_user_dir}
	log_info $method_name "uploaded file: $sus_jar_path to path on server: $sus_root_user_dir"

	log_info $method_name "uploading file: $sus_utils_sh_path to path on server: $sus_root_user_dir"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ft-client ${sus_host}#${sus_port} ${sus_utils_sh_path} ${sus_root_user_dir}
	log_info $method_name "uploaded file: $sus_utils_sh_path to path on server: $sus_root_user_dir"

	log_info $method_name "uploading file: $sus_utils_stp_sh_path to path on server: $sus_root_user_dir"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ft-client ${sus_host}#${sus_port} ${sus_utils_stp_sh_path} ${sus_root_user_dir}
	log_info $method_name "uploaded file: $sus_utils_stp_sh_path to path on server: $sus_root_user_dir"

	log_info $method_name "changing the permission for files ${sus_root_user_dir}/${sus_utils_sh_file_name}"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "chmod 755 ${sus_root_user_dir}/${sus_utils_sh_file_name}"
	log_info $method_name "changed the permission for files ${sus_root_user_dir}/${sus_utils_sh_file_name}"

	log_info $method_name "changing the permission for files ${sus_root_user_dir}/${sus_utils_st_sh_file_name}"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "chmod 755 ${sus_root_user_dir}/${sus_utils_st_sh_file_name}"
	log_info $method_name "changed the permission for files ${sus_root_user_dir}/${sus_utils_st_sh_file_name}"
	
	log_info $method_name "changing the permission for files ${sus_root_user_dir}/${sus_jar_file_name}"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "chmod 755 ${sus_root_user_dir}/${sus_jar_file_name}"
	log_info $method_name "changed the permission for files ${sus_root_user_dir}/${sus_jar_file_name}"
		
	log_info $method_name "listing remote server path: $sus_root_user_dir"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "ls -lrt ${sus_root_user_dir}"

	log_info $method_name "restarting the $sus_host for user: $sus_user on port: $sus_port"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "${sus_root_user_dir}/${sus_utils_sh_file_name} restart_sus $sus_user $sus_port"
	log_info $method_name "restarted the $sus_host for user: $sus_user on port: $sus_port"

	return_value $method_name 0
	#log_info $method_name "end"
}

#the cmd_client $1 - the ip#port, $2 - the user, $3 - the command or remoted shell scripts file path, return 0 when success.
cmd_client() {
	local method_name
	local sus_ip_port
	local sus_user
	local sus_cmd
	local sus_client_type
	local sus_jar_path
	local sus_skey_file

	method_name="cmd_client"
	log_info $method_name "start"

	is_args_count_valid_ge $# 3 "arguments expected (or more than 3 as cmd arguments): ip#port sus_user cmd"

	sus_ip_port=$1
	sus_user=$2
	#get all arguments from $3
	shift
	shift
	sus_cmd="$@"
	sus_client_type="cmd-client"

	log_info "going to run cmd: ${sus_cmd} on host: $sus_ip_port with user: $sus_user"

	#confirm to continue
	get_user_confirmation "are you sure want to proceed? y/n"

	#configure accordingly to current env
	sus_jar_path="${sus_root}/${sus_user}/${sus_jar_file_name}"
	sus_jar_path=$(get_cygpath $sus_jar_path)

	sus_skey_file="${sus_root}/${sus_user}/skey_file_${sus_user}.txt"
	sus_skey_file=$(get_cygpath $sus_skey_file)

	log_info $method_name "start running cmd: $sus_cmd"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ${sus_client_type} ${sus_ip_port} "${sus_cmd}"
	log_info $method_name "done running cmd: $sus_cmd"

 	return_value $method_name 0
	#log_info $method_name "end"

}

#the ft_client $1 - the ip#port, $2 - the user, $3 - the file to upload, $4 - the target path on remote server, return 0 when success.
ft_client() {
	local method_name
	local sus_ip_port
	local sus_user
	local sus_file_to_upload
	local sus_path_on_remote
	local sus_client_type
	local sus_jar_path
	local sus_skey_file

	method_name="ft_client"
	log_info $method_name "start"

	is_args_count_valid $# 4 "arguments expected: ip#port sus_user file_to_upload path_on_remote_server"

	sus_ip_port=$1
	sus_user=$2
	sus_file_to_upload=$3
	sus_path_on_remote=$4
	sus_client_type="ft-client"

	log_info "going to upload file: ${sus_file_to_upload} to host: $sus_ip_port on path: ${sus_path_on_remote} with user: $sus_user"

	#confirm to continue
	get_user_confirmation "are you sure want to proceed? y/n"

	#configure accordingly to current env
	sus_jar_path="${sus_root}/${sus_user}/${sus_jar_file_name}"
	sus_jar_path=$(get_cygpath $sus_jar_path)

	sus_skey_file="${sus_root}/${sus_user}/skey_file_${sus_user}.txt"
	sus_skey_file=$(get_cygpath $sus_skey_file)

	log_info $method_name "start uploading file: $sus_file_to_upload"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ${sus_client_type} ${sus_ip_port} ${sus_file_to_upload} ${sus_path_on_remote}
	log_info $method_name "uploaded file: $sus_file_to_upload"

	return_value $method_name 0
	#log_info $method_name "end"

}

##use to upgrade all servers (dev/sit/uat) $1 - sus_user, $2 - sus_port, return 0 on success.
upgrade_sus_all() {
	local method_name
	local sus_user
	local sus_port

	method_name="upgrade_sus_all"
	log_info $method_name "start"

	is_args_count_valid $# 2 "arguments expected: sus_user sus_port"

	sus_user=$1
	sus_port=$2

	#configure accordingly for list of servers to upgrade
	#host_list="192.168.35.226 192.168.74.32 192.168.74.33 192.168.39.143 192.168.39.131 192.168.39.156"
	#confirm to continue
	get_user_confirmation "are you sure want to upgrade for list of hosts: $host_list for user: $sus_user on port: $sus_port? y/n"
	
	for host in $host_list; do
		log_info $method_name "upgrading for host: $host with user: $sus_user on port: $sus_port"
		upgrade_sus $host $sus_user $sus_port
		log_info $method_name "upgraded for host: $host with user: $sus_user on port: $sus_port"
	done

	return_value $method_name 0
	#log_info $method_name "end"
}

##use to run cmd_client on all servers (dev/sit/uat) $1 - sus_user, $2 - sus_port, $3 - cmd, return 0 on success.
cmd_client_all() {
	local method_name
	local sus_user
	local sus_port
	local cmd

	method_name="cmd_client_all"
	log_info $method_name "start"

	is_args_count_valid_ge $# 3 "arguments expected (or more as cmd arguments): sus_user sus_port cmd"

	sus_user=$1
	sus_port=$2
	#get all arguments from $3
	shift
	shift
	cmd="$@"

	#configure accordingly for list of servers to upgrade
	host_list="192.168.74.32 192.168.39.218"
	#confirm to continue
	get_user_confirmation "are you sure want to run cmd_client for list of hosts: $host_list for user: $sus_user on port: $sus_port cmd: $cmd? y/n"

	for host in $host_list; do
		log_info $method_name "running cmd_client for host: $host with user: $sus_user on port: $sus_port for cmd: $cmd"
		cmd_client $host#$sus_port $sus_user "$cmd"
		log_info $method_name "executed for host: $host with user: $sus_user on port: $sus_port for cmd: $cmd"
	done

	return_value $method_name 0
	#log_info $method_name "end"
}

##use to run ft_client on all servers (dev/sit/uat) $1 - sus_user, $2 - sus_port, $3 - file, $4 - target foder on server, return 0 on success.
ft_client_all() {
	local method_name
	local sus_user
	local sus_port
	local file_to_send
	local folder_to_place

	method_name="ft_client_all"
	log_info $method_name "start"

	is_args_count_valid $# 4 "arguments expected: sus_user sus_port file_to_send folder_to_place"

	sus_user=$1
	sus_port=$2
	file_to_send=$3
	folder_to_place=$4

	#configure accordingly for list of servers to upgrade
	#host_list="192.168.35.226 192.168.39.131"
	#confirm to continue
	get_user_confirmation "are you sure to run ft_client for hosts: $host_list for user: $sus_user on port: $sus_port file_to_send: $file_to_send folder_to_place: $folder_to_place? y/n"

	for host in $host_list; do
		log_info $method_name "running ft_client for host: $host with user: $sus_user on port: $sus_port for file_to_send: $file_to_send"
		ft_client $host#$sus_port $sus_user $file_to_send $folder_to_place
		log_info $method_name "done ft_client for host: $host with user: $sus_user on port: $sus_port for file_to_send: $file_to_send"
	done

	return_value $method_name 0
	#log_info $method_name "end"
}

##use to cleanup (run as local command) the sus backups on this server $1 - sus_user, $2 - sus_keyword return 0 on success.
sus_clean_up() {
	local method_name
	local sus_user
	local keyword
	local sus_dir

	method_name="sus_clean_up"
	log_info $method_name "start"

	is_args_count_valid $# 2 "arguments expected: sus_user sus_keyword"

	sus_user=$1
	sus_keyword="*$2*"
	
	sus_dir=${sus_root}/${sus_user}
	log_info $method_name "running clean up for sus_dir: $sus_dir with sus_keyword: ${sus_keyword}"
	log_info $method_name "before list files from ${sus_dir}" 
	ls -lrt ${sus_dir}

	log_info $method_name "before list files to be deleted from ${sus_dir}" 
	ls -lrt ${sus_dir}/${sus_keyword}

	#confirm to continue
	#get_user_confirmation "are you sure want to clean up for above files? y/n"
	rm -rf ${sus_dir}/${sus_keyword}

	log_info $method_name "after list files to be deleted from ${sus_dir}" 
	ls -lrt ${sus_dir}/${sus_keyword}
		
	log_info $method_name "after list files from ${sus_dir}" 
	ls -lrt ${sus_dir}

	log_info $method_name "done clean up for sus_dir: $sus_dir with sus_keyword: ${sus_keyword}"

	return_value $method_name 0
	#log_info $method_name "end"
}

##use to deploy the sus on this server for the sus_user (and start the server on the port) , $1 - sus_user, $2 - sus_port, $3 - copy_from_sus_user return 0 on success.
deploy_sus() {
	local method_name
	local sus_user
	local sus_port

	method_name="deploy_sus"
	log_info $method_name "start"

	is_args_count_valid $# 3 "arguments expected: sus_user sus_port copy_from_sus_user"

	sus_user=$1
	sus_port=$2
	copy_from_sus_user=$3

	is_user_allowed $sus_user

	local sus_dir=${sus_root}/${sus_user}
	ls -lrt ${sus_dir}
	log_info $method_name "creating sus_dir: $sus_dir"
	mkdir -p $sus_dir
	log_info $method_name "created sus_dir: $sus_dir"

	log_info $method_name "before list files from ${sus_dir}"
	ls -lrt $sus_dir
	log_info $method_name "before copying files from ${sus_root}/${copy_from_sus_user}/*.jar *.sh to ${sus_dir}"
	cp ${sus_root}/${copy_from_sus_user}/*.jar $sus_dir
	cp ${sus_root}/${copy_from_sus_user}/*.sh $sus_dir
	log_info $method_name "after copying files from ${sus_root}/${copy_from_sus_user}/*.jar *.sh to ${sus_dir}"
	log_info $method_name "after list files from ${sus_dir}"
	ls -lrt $sus_dir
	
	log_info $method_name "starting the sus for sus_user: $sus_user on port: $sus_port"
	start_sus $sus_user $sus_port
	log_info $method_name "started the sus for sus_user: $sus_user on port: $sus_port"
	
	log_info $method_name "#########################"
	log_info $method_name "make sure to upload the skey_file_${sus_user}.txt to path: $sus_dir, and issue ${sus_dir}/${sus_utils_sh_file_name} restart_sus $sus_user $sus_port"
	log_info $method_name "#########################"
	
	return_value $method_name 0
	#log_info $method_name "end"
}

##entry point here
log_info "entry_point" "the arguments received: $*"

#min arguments check
if [ "$#" -lt 1 ] ; then
	echo "$the_usage"
	exit 1
fi

my_option=$1

shift
my_option_args="$@"
log_info "entry_point" "my_option: $my_option"
log_info "entry_point" "the my_option_args received: $my_option_args"
case $my_option in 
	find_sus_pid) find_sus_pid $my_option_args
	;;
	kill_sus) kill_sus $my_option_args
	;;
	start_sus) start_sus $my_option_args
	;;
	restart_sus) restart_sus $my_option_args
	;;
	cmd_client) cmd_client $my_option_args
	;;
	ft_client) ft_client $my_option_args
	;;
	upgrade_sus) upgrade_sus $my_option_args
	;;
	upgrade_sus_all) upgrade_sus_all $my_option_args
	;;
	cmd_client_all) cmd_client_all $my_option_args
	;;
	ft_client_all) ft_client_all $my_option_args
	;;
	sus_clean_up) sus_clean_up $my_option_args
	;;
	deploy_sus) deploy_sus $my_option_args
	;;

	*) echo "$the_usage"
	;;
esac

#reset the trace
set +x

