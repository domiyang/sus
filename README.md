#sus [![Build Status](https://travis-ci.org/domiyang/sus.svg?branch=master)](https://travis-ci.org/domiyang/sus)
This is a simple java socket utils containing some useful functions like file transfer, remote command execution etc. 

##features
- run as server
- run as repeater (forward request to next server/repeater ...)
- run as client
- made from single java file for easier deployment 

##how to use

###run as jar
<pre>
can be run in different mode as below:
- Client (send file to server): java -jar sus.jar ft-client _ip#_port _file [_dirDest]
- Client (send file to server via repeater): java -jar sus.jar ft-client _ip#_port[~_~_ip2#_port2...] _file  [_dirDest]
- Client (send cmd to server): java -jar sus.jar cmd-client _ip#_port _cmd
- Client (send cmd to server via repeater): java -jar sus.jar cmd-client _ip#_port[~_~_ip2#_port2...] _cmd
- Server: java -Xms64m -Xmx256m -jar sus.jar server _port _dir
- Client/Server (secure with secret key) add the jvm arguments as: java -DsecretKeyFile=d:/tools/sus/my_skey.txt
</pre>

###run as java
<pre>
can be run in different mode as below:
- Client (send file to server): java com.dmb.tools.sus.SocketUtilApp ft-client _ip#_port _file [_dirDest]
- Client (send file to server via repeater): java com.dmb.tools.sus.SocketUtilApp ft-client _ip#_port[~_~_ip2#_port2...] _file  [_dirDest]
- Client (send cmd to server): java com.dmb.tools.sus.SocketUtilApp cmd-client _ip#_port _cmd
- Client (send cmd to server via repeater): java com.dmb.tools.sus.SocketUtilApp cmd-client _ip#_port[~_~_ip2#_port2...] _cmd
- Server: java -Xms64m -Xmx256m com.dmb.tools.sus.SocketUtilApp server _port _dir
- Client/Server (secure with secret key) add the jvm arguments as: java -DsecretKeyFile=d:/tools/sus/my_skey.txt
</pre>

##FAQ

###Q1: why it's made from a single java file?
Made from a single java file make it easier to deploy to server (copy the content, and compile from server) specially when there has issue do the initial upload of file/jar.
