#!/bin/sh
#
#- Description: 	This is the socket utils scripts help to manage the socket util jar upgrade process.
#- Date:			11-Dec-2016
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

#enforce execution stauts check
set -x

#ps -ef | grep $ps_keyward_sus to find the running process for sus server
ps_keyward_sus=sus.jar

#skip user confirmation
skip_user_confirmation=n

#skip current user check
skip_current_user_check=n

#sus jar file name
sus_jar_file_name=sus.jar

#log file
run_log_file=`basename $0`_$(date +%Y-%m-%d_%H%M%S).log

#common configurations
java_executable_path=/sus-app/domi/java

#the remote sus root path
sus_root=/sus-app

#the local sus root path
sus_root_local="d:/tools/sus"

#list of servers to upgrade for sus if required
host_list="1.1.1.2 1.1.1.3 1.1.1.4 1.1.1.5 1.1.1.6"

#shared global variables
sus_pid=""

#some helper utils
#common log method with common prefixing. $1 - the prefix, $2 - the original calling method name $3 - the original message.
log_common(){
	local current_date_time
	current_date_time=$(date +%Y-%m-%d_%H:%M:%S)
	echo "${current_date_time}|$1|$2|$3" >> $run_log_file
}

#log the error message $1 - the calling method name, $2 the msg
log_error(){
	log_common "ERROR" "$1" "$2"
}

#log the info message $1 - the calling method name, $2 the msg
log_info(){
	if [ $# -eq 2 ] ; then
        log_common "INFO" "$1" "$2"
	else
        log_common "INFO" "" "$1"
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
    	if [ "y" != "$skip_current_user_check" ] ; then
        	exit_code $method_name 2
        else
        	log_info $method_name "skipping current user check: $skip_current_user_check"
        fi
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

#convert the path if it's running in CYGWIN mode on windows, $1 - the original path, return by echoing the original/converted path if required.
get_cygpath(){
	local method_name

	method_name="get_cygpath"
	#log_info $method_name "start"

    if [ "$(expr substr $(uname -s) 1 6)" == "CYGWIN" ] ; then
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
    
    if [ "y" != "$skip_user_confirmation" ] ; then
    	echo "not skipping"
    
	    read user_response
	    if [ "y" != "$user_response" ] ; then
	    	log_info $method_name "existing the current process."
	    	exit_code $method_name 2  
	    fi
	else
		log_info $method_name "skipping user confirmation: $skip_user_confirmation"
	fi
}

#kill the process for $ps_keyward_sus for the port (port of the process is unique), $1 - the sus_port, exit 0 when success.
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

#find the process id for the $ps_keyward_sus for the port, $1 - the sus_port, return 0 and set the sus_pid shared variable, return 2 otherwise.
find_sus_pid() {
	local method_name

	method_name="find_sus_pid"

	log_info $method_name "start"

	is_args_count_valid $# 1 "arguments expected: sus_port"

	sus_port=$1
    log_info $method_name "finding sus_pid for port: $sus_port"
    ps -ef | grep "$ps_keyward_sus" | grep "$sus_port"
	#setting the global shared variables
    sus_pid=`ps -ef | grep "$ps_keyward_sus" | grep "$sus_port" | awk -F " " '{print $2}'`
	
	if [ "" == "$sus_pid" ] ; then
		log_error $method_name "not found sus_pid for port: $sus_port"
		return_value $method_name 2
	else
		log_info "found sus_pid=$sus_pid for port: $sus_port"
		return_value $method_name 1
	fi
	
	#log_info $method_name "end"
}

#start the sus server in the background, $1 - the system user to run with (sususr/susdp1/suswas), $2 - the port sus_server to listening on (1983,1984,1985), return 0 when started successfully, 2 otherwise.
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
    ps -ef | grep "$ps_keyward_sus" | grep "$sus_port"
	
	log_info $method_name "going to start sus server with user: $sus_user on port: $sus_port"
	nohup ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} server ${sus_port} ${sus_dir} >> ${sus_log_file} 2>&1 &

    log_info $method_name "ps after start sus for sus_port: $sus_port"
    ps -ef | grep "$ps_keyward_sus" | grep "$sus_port"
	
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
	local sus_skey_file
	local sus_jar_dir

	method_name="upgrade_sus"
    log_info $method_name "start"
	
	is_args_count_valid $# 3 "arguments expected: sus_host sus_user sus_port"

    sus_host=$1
    sus_user=$2
    sus_port=$3

    log_info "going to upgrade sus with host: $sus_host user: $sus_user on port: $sus_port sus_jar: $sus_jar_path sus_utils_sh_path: $sus_utils_sh_path sus_skey_file: $sus_skey_file"

	#confirm to continue
    get_user_confirmation "are you sure want to upgrade the hosts: $sus_host for user: $sus_user on port: $sus_port? y/n"

    #configure accordingly to local env
    sus_jar_path="${sus_root_local}/${sus_jar_file_name}"
    sus_utils_sh_path="${sus_root_local}/sus_utils.sh"
    sus_skey_file="${sus_root_local}/my_key.txt"

    #the server sus installation path
    sus_jar_dir=${sus_root}/${sus_user}

	log_info $method_name "uploading file: $sus_jar_path to path on server: $sus_jar_dir"
    ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ft-client ${sus_host}#${sus_port} ${sus_jar_path} ${sus_jar_dir}
	log_info $method_name "uploaded file: $sus_jar_path to path on server: $sus_jar_dir"

	log_info $method_name "uploading file: $sus_utils_sh_path to path on server: $sus_jar_dir"
    ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} ft-client ${sus_host}#${sus_port} ${sus_utils_sh_path} ${sus_jar_dir}
    log_info $method_name "uploaded file: $sus_utils_sh_path to path on server: $sus_jar_dir"

	log_info $method_name "changing the permission for files ${sus_jar_dir}/sus_utils.sh"
    ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "chmod 755 ${sus_jar_dir}/sus_utils.sh"
	log_info $method_name "changed the permission for files ${sus_jar_dir}/sus_utils.sh"
	
	log_info $method_name "changing the permission for files ${sus_jar_dir}/${sus_jar_file_name}"
    ${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "chmod 755 ${sus_jar_dir}/${sus_jar_file_name}"
    log_info $method_name "changed the permission for files ${sus_jar_dir}/${sus_jar_file_name}"

	log_info $method_name "listing remote server path: $sus_jar_dir"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "ls -lrt ${sus_jar_dir}"

	log_info $method_name "restarting the $sus_host for user: $sus_user on port: $sus_port"
	${java_executable_path} -DsecretKeyFile=${sus_skey_file} -jar ${sus_jar_path} cmd-client ${sus_host}#${sus_port} "${sus_jar_dir}/sus_utils.sh restart_sus $sus_user $sus_port"
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

    is_args_count_valid $# 3 "arguments expected: ip#port sus_user cmd"

    sus_ip_port=$1
    sus_user=$2
    sus_cmd="$3"
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
	#host_list="1.1.35.226 1.1.74.32 1.1.74.33 1.1.39.143 1.1.39.131 1.1.39.156"
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

    is_args_count_valid $# 3 "arguments expected: sus_user sus_port cmd"

    sus_user=$1
    sus_port=$2
    cmd=$3

    #configure accordingly for list of servers to upgrade
    #host_list="1.1.35.226 1.1.39.131"
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
    #host_list="1.1.35.226 1.1.39.131"
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

##use to cleanup (run as local command) the sus backups on this server $1 - sus_user return 0 on success.
sus_clean_up() {
    local method_name
    local sus_user
    local keyword
    local sus_dir

    method_name="sus_clean_up"
    log_info $method_name "start"

    is_args_count_valid $# 1 "arguments expected: sus_user"

    sus_user=$1
    keyword='*_2016*'

	sus_dir=${sus_root}/${sus_user}
    log_info $method_name "running clean up for sus_dir: $sus_dir with keyword: ${keyword}"
	log_info $method_name "before list files from ${sus_dir}" 
    ls -lrt ${sus_dir}

	log_info $method_name "before list files to be deleted from ${sus_dir}" 
    ls -lrt ${sus_dir}/${keyword}

	#confirm to continue
    #get_user_confirmation "are you sure want to clean up for above files? y/n"
    rm -rf ${sus_dir}/${keyword}

	log_info $method_name "after list files to be deleted from ${sus_dir}" 
    ls -lrt ${sus_dir}/${keyword}
		
	log_info $method_name "after list files from ${sus_dir}" 
    ls -lrt ${sus_dir}

    log_info $method_name "done clean up for sus_dir: $sus_dir with keyword: ${keyword}"

    return_value $method_name 0
    #log_info $method_name "end"
}

##use to deploy the sus on this server for the sus_user (and start the server on the port) , $1 - sus_user, $2 - sus_port return 0 on success.
deploy_sus() {
    local method_name
    local sus_user
    local sus_port

    method_name="deploy_sus"
    log_info $method_name "start"

    is_args_count_valid $# 2 "arguments expected: sus_user sus_port"

    sus_user=$1
    sus_port=$2

    is_user_allowed $sus_user

    local sus_dir=${sus_root}/${sus_user}
    ls -lrt ${sus_dir}
    log_info $method_name "creating sus_dir: $sus_dir"
	mkdir -p $sus_dir
    log_info $method_name "created sus_dir: $sus_dir"

    log_info $method_name "before list files from ${sus_dir}"
	ls -lrt $sus_dir
	log_info $method_name "before copying files from ${sus_root}/sususr/*.jar *.sh to ${sus_dir}"
	cp ${sus_root}/sususr/*.jar $sus_dir
	cp ${sus_root}/sususr/*.sh $sus_dir
	log_info $method_name "after copying files from ${sus_root}/sususr/*.jar *.sh to ${sus_dir}"
    log_info $method_name "after list files from ${sus_dir}"
	ls -lrt $sus_dir
    
	log_info $method_name "starting the sus for sus_user: $sus_user on port: $sus_port"
	start_sus $sus_user $sus_port
	log_info $method_name "started the sus for sus_user: $sus_user on port: $sus_port"
	
	log_info $method_name "#########################"
	log_info $method_name "make sure to upload the skey_file_${sus_user}.txt to path: $sus_dir, and issue ${sus_dir}/sus_utils.sh restart_sus $sus_user $sus_port"
	log_info $method_name "#########################"
    
	return_value $method_name 0
    #log_info $method_name "end"
}

##entry point here
my_option=$1
case $my_option in 
	find_sus_pid) find_sus_pid $2
	;;
	kill_sus) kill_sus $2
	;;
	start_sus) start_sus $2 $3
    ;;
	restart_sus) restart_sus $2 $3
    ;;
	cmd_client) cmd_client $2 $3 "$4"
    ;;
	ft_client) ft_client $2 $3 $4 $5
    ;;
	upgrade_sus) upgrade_sus $2 $3 $4
    ;;
	upgrade_sus_all) upgrade_sus_all $2 $3
    ;;
	cmd_client_all) cmd_client_all $2 $3 "$4"
    ;;
	ft_client_all) ft_client_all $2 $3 $4 $5
    ;;
	sus_clean_up) sus_clean_up $2
    ;;
	deploy_sus) deploy_sus $2 $3
    ;;
	*) echo "$the_usage"
	;;
esac
