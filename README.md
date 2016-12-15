# sus [![Build Status](https://travis-ci.org/domiyang/sus.svg?branch=master)](https://travis-ci.org/domiyang/sus)
This is a java socket utils containing some useful functions like file upload, remote command execution etc.

##how to use
<pre>
can be run in different mode as below:
- Client (send file to server): java -jar sus.jar ft-client _ip#_port _file [_dirDest]
- Client (send file to server via repeater): java -jar sus.jar ft-client _ip#_port[~_~_ip2#_port2...] _file  [_dirDest]
- Client (send cmd to server): java -jar sus.jar cmd-client _ip#_port _cmd
- Client (send cmd to server via repeater): java -jar sus.jar cmd-client _ip#_port[~_~_ip2#_port2...] _cmd
- Server: java -Xms64m -Xmx256m -jar sus.jar server _port _dir
- Client/Server (secure with secret key) add the jvm arguments as: java -DsecretKeyFile=d:/tools/sus/my_skey.txt
</pre>
