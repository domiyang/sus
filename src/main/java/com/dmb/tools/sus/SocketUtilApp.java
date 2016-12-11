/*
- Description: 	This socket app contains many useful functions that working with java socket. e.g.: file transfer, run remote command, etc.
- Date:			11-Dec-2016
- Author: 		Domi Yang (domi_yang@hotmail.com)
---------------------------------------------------------------------------
   Copyright 2016 Domi Yang (domi_yang@hotmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
---------------------------------------------------------------------------
 */

package com.dmb.tools.sus;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This socket app contains many useful functions that working with java socket.
 * e.g.: file transfer, run remote command, etc.
 * @author Domi Yang
 */
public class SocketUtilApp {
	public static final int CMD_EXE_WAITING_TIME_SECONDS = 60;
	public static final String RESPONSE_NULL = "null response received from server!";
	public static final String RESPONSE_ACCESS_DENIED = "sKey not matched, access denied!";
	public static final String SECRET_KEY_FILE = "secretKeyFile";
	public static final String ROUTER_ENTRY_DELIMETER = "~_~";

	public static final String CD_KEY_SKEY = "sKey";
	public static final String CD_KEY_CMD = "cmd";
	public static final String CD_KEY_REQUEST_MODE = "requestMode";
	public static final String CD_KEY_ROUTER_LIST = "routerList";
	public static final String CD_KEY_FILE_SIZE = "fileSize";
	public static final String CD_KEY_DIR_DEST = "dirDest";
	public static final String CD_KEY_FILE_NAME = "fileName";

	public static final String MASKING_LOG_PATTERN = "(sKey=)([\\w]*)";
	public static final String MASKING = "******";

	public static final String IP_PORT_DELIMETER = "#";
	public static final boolean LOG_LEVEL_DEBUG = false;
	public static final String LOG_TYPE_DEBUG = "DEBUG";
	public static final String LOG_TYPE_INFO = "INFO";
	public static final String LOG_TYPE_ERROR = "ERROR";
	public static final String LOG_TYPE_WARN = "WARN";
	public static final int PORT_DEFAULT = 1983;
	public static final int CONTROL_DATA_BYTES = 256;
	public static final char PADDING_CHAR = '<';
	public static final String DATE_TIME_FORMAT_LOG = "yyyy-MM-dd'_'HH:mm:ss.SSS";
	public static final String DATE_TIME_FORMAT_FILE = "yyyyMMddHHmmss";
	public static final String APP_NAME = SocketUtilApp.class.getName();
	public static final String APP_NAME_SIMPLE = SocketUtilApp.class.getSimpleName();

	public static final String MODE_FT_CLIENT = "ft-client";
	public static final String MODE_CMD_CLIENT = "cmd-client";
	public static final String MODE_SERVER = "server";
	public static final List<String> MODE_LIST = Arrays.asList(new String[] { MODE_SERVER, MODE_FT_CLIENT, MODE_CMD_CLIENT });

	public static String MODE = "mode";
	public static final String VERSION = "0.1";

	public static void main(String[] args) throws Exception {
		startProcess(args);
	}

	/**
	 * Print the usage and exit the current jvm with status code 1.
	 */
	private static void printUsage() {
		final String METHOD_NAME = "printUsage()";

		String javaCmdSimple = "java ";
		String javaCmdComplex = "java -Xms64m -Xmx256m ";
		String javaCmdSecure = "java -D" + SECRET_KEY_FILE + "=" + "d:/tools/my_key.txt";

		logInfo(METHOD_NAME, "####USER GUIDE START####");
		logInfo(METHOD_NAME, "Client (send file to server): " + javaCmdSimple + APP_NAME + " " + MODE_FT_CLIENT + " _ip#_port _file [_dirDest]");
		logInfo(METHOD_NAME, "Client (send file to server via repeater): " + javaCmdSimple + APP_NAME + " " + MODE_FT_CLIENT + " _ip#_port[" + ROUTER_ENTRY_DELIMETER
				+ "_ip2#_port2...] _file  [_dirDest]");
		logInfo(METHOD_NAME, "Client (send cmd to server): " + javaCmdSimple + APP_NAME + " " + MODE_CMD_CLIENT + " _ip#_port _cmd");
		logInfo(METHOD_NAME, "Client (send cmd to server via repeater): " + javaCmdSimple + APP_NAME + " " + MODE_CMD_CLIENT + " _ip#_port[" + ROUTER_ENTRY_DELIMETER
				+ "_ip2#_port2...] _cmd");
		logInfo(METHOD_NAME, "Server: " + javaCmdComplex + APP_NAME + " " + MODE_SERVER + " _port _dir");

		logInfo(METHOD_NAME, "Client/Server (secure with secret key) add the jvm arguments as: " + javaCmdSecure);

		logInfo(METHOD_NAME, "####USER GUIDE END####");

		throw new RuntimeException("Please run with expected arguments.");
	}

	/**
	 * Start processing from here.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static String startProcess(String[] args) {
		final String METHOD_NAME = "startProcess(...)";
		String response = null;

		Map<String, String> cdMap = new HashMap<String, String>();

		if (args.length == 0) {
			printUsage();
		}

		int argsCount = args.length;

		String mode = args[0];

		logInfo(METHOD_NAME, "argsCount/args=" + argsCount + "/" + getStringForStringArray(args));

		String routerList = null;
		String myPort = null;
		String file = null;
		String dir = null;
		String dirDest = null;

		String cmd = null;

		String sKey = getTheSecretKey();
		cdMap.put(CD_KEY_SKEY, sKey);

		// check if it's valid mode
		if (!MODE_LIST.contains(mode)) {
			printUsage();
		}

		// for logging prefix
		MODE = mode;

		// set common fields into cdMap
		cdMap.put(CD_KEY_REQUEST_MODE, mode);

		// ft client mode
		if (MODE_FT_CLIENT.equals(mode)) {
			if (!(argsCount == 3 || argsCount == 4)) {
				printUsage();
			}

			routerList = args[1];
			file = args[2];

			// the client specified destination folder path
			if (argsCount == 4) {
				dirDest = args[3];
			}

			cdMap.put(CD_KEY_ROUTER_LIST, routerList);
			cdMap.put(CD_KEY_DIR_DEST, dirDest);

			response = sendFileToServer(cdMap, file);
			checkResponse(response);
		}

		// cmd client mode
		if (MODE_CMD_CLIENT.equals(mode)) {
			if (argsCount != 3) {
				printUsage();
			}

			routerList = args[1];
			cmd = args[2];
			cdMap.put(CD_KEY_ROUTER_LIST, routerList);
			cdMap.put(CD_KEY_CMD, cmd);
			response = sendCmdToServer(cdMap);
			checkResponse(response);
		}

		// common server mode
		if (MODE_SERVER.equals(mode)) {
			if (argsCount != 3) {
				printUsage();
			}

			myPort = args[1];
			dir = args[2];
			createServer(myPort, dir, cdMap);
		}

		return response;

	}

	/**
	 * Load and return the secret key from the jvm argument specific file:
	 * SECRET_KEY_FILE. Return null when not set.
	 * 
	 * @return
	 */
	private static String getTheSecretKey() {
		final String METHOD_NAME = "getTheSecretKey()";
		String sKey = null;
		String secretKeyFile = System.getProperty(SECRET_KEY_FILE);
		logInfo(METHOD_NAME, "secretKeyFile=" + secretKeyFile);
		if (isNotBlank(secretKeyFile)) {
			File file = new File(secretKeyFile);
			if (!file.exists() || !file.canRead()) {
				logWarn(METHOD_NAME, "secretKeyFile not available, not set sKey=" + sKey);
				return null;
			}

			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
				sKey = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				handleStreamClosing(br);
			}

			// check the sKey
			if (isNotBlank(sKey)) {
				logInfo(METHOD_NAME, "sKey=" + sKey);
			}
		} else {
			logWarn(METHOD_NAME, "not set sKey=" + sKey);
		}

		return sKey;
	}

	private static String getMaskedStringForLogging(String keyValStr) {

		if (isNotBlank(keyValStr)) {
			return keyValStr.replaceAll(MASKING_LOG_PATTERN, "$1" + MASKING);
		} else {
			return keyValStr;
		}
	}

	/**
	 * Get the port number from string, default to PORT_DEFAULT when invalid
	 * from input.
	 * 
	 * @param myPort
	 * @return
	 */
	private static int getPortNum(String myPort) {
		final String METHOD_NAME = "getPortNum(...)";
		int port = 0;
		try {
			port = Integer.parseInt(myPort);
		} catch (Exception e) {
			logError(METHOD_NAME, "defaulting to port: " + PORT_DEFAULT);
			port = PORT_DEFAULT;
		}

		return port;
	}

	/**
	 * Send the file to the target server via routerList (in format
	 * ip#port~_~ip2#port2...), will stored on dirDest folder on server if value
	 * specified.
	 * 
	 * @param routerList
	 * @param file
	 * @param dirDest
	 * @return
	 */
	private static String sendFileToServer(Map<String, String> cdMap, String file) {
		final String METHOD_NAME = "sendFileToServer(...)";

		String routerList = cdMap.get(CD_KEY_ROUTER_LIST);
		String dirDest = cdMap.get(CD_KEY_DIR_DEST);

		String ip = null;
		String myPort = null;
		int port = 0;

		String response = null;

		if (isNotBlank(routerList)) {
			String[] ipPortStrList = routerList.split(ROUTER_ENTRY_DELIMETER);

			updateRouterList(cdMap, routerList);

			String[] ipPortStr = ipPortStrList[0].split(IP_PORT_DELIMETER);
			ip = ipPortStr[0];
			myPort = ipPortStr[1];
			port = getPortNum(myPort);
			logInfo(METHOD_NAME, "ip/myPort/port=" + ip + "/" + myPort + "/" + port);

		} else {
			logError(METHOD_NAME, "invalid routerList=" + routerList);
			response = "invalid routerList=" + routerList;

			logError(METHOD_NAME, "Response," + response);
			return response;
		}

		Socket client = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;

		BufferedInputStream bis = null;

		try {

			client = new Socket(ip, port);

			String serverIp = handleIpAddress(client.getRemoteSocketAddress().toString(), IP_PORT_DELIMETER);
			String localIp = handleIpAddress(client.getLocalSocketAddress().toString(), IP_PORT_DELIMETER);

			logInfo(METHOD_NAME, "Request," + localIp + "=>" + serverIp + ",file=" + file);

			File inFile = new File(file);

			int inFileLength = (int) inFile.length();

			byte[] outBytes = new byte[CONTROL_DATA_BYTES + inFileLength];

			String fileName = inFile.getName();
			logInfo(METHOD_NAME, "fileName=" + fileName);
			cdMap.put(CD_KEY_FILE_NAME, fileName);

			logInfo(METHOD_NAME, "dirDest=" + dirDest);

			String fileSizeStr = Integer.toString(inFileLength);
			logInfo(METHOD_NAME, "fileSizeStr=" + fileSizeStr);
			cdMap.put(CD_KEY_FILE_SIZE, fileSizeStr);

			updateControlDataBytes(outBytes, cdMap);

			bis = new BufferedInputStream(new FileInputStream(inFile));
			bis.read(outBytes, CONTROL_DATA_BYTES, inFileLength);

			logInfo(METHOD_NAME, "Sending..." + localIp + "=>" + serverIp + ",file::size(bytes)=" + file + "::" + inFileLength);
			// send to server
			dos = new DataOutputStream(client.getOutputStream());
			dos.write(outBytes);
			dos.flush();

			logInfo(METHOD_NAME, "Sent," + localIp + "=>" + serverIp + ",file::size(bytes)=" + file + "::" + inFileLength);

			logInfo(METHOD_NAME, "Waiting for response..." + serverIp + "=>" + localIp);
			// read the response from server
			dis = new DataInputStream(client.getInputStream());

			response = dis.readUTF();

			logInfo(METHOD_NAME, "Response," + serverIp + "=>" + localIp + ",ft_rs=" + response);

		} catch (Throwable t) {
			t.printStackTrace();
			response = t.toString();
		} finally {
			handleStreamClosing(bis);
			handleStreamClosing(dos);
			handleStreamClosing(dis);
			handleStreamClosing(client);
		}

		return response;
	}

	private static void appendStringToBytesArray(byte[] outBytes, String valStr, int offset, int length) {
		final String METHOD_NAME = "appendStringToBytesArray(...)";

		logInfo(METHOD_NAME, "outBytes.length/valStr/offset/length=" + outBytes.length + "/" + valStr + "/" + offset + "/" + length);
		if (null == valStr) {
			valStr = "";
			logWarn(METHOD_NAME, "update valStr from null to: " + valStr);
		}

		byte[] valStrBytes = valStr.getBytes(Charset.forName("UTF-8"));
		logInfo(METHOD_NAME, "valStrBytes.length=" + valStrBytes.length);

		for (int i = offset; i < valStrBytes.length + offset; i++) {
			logDebug(METHOD_NAME, "i=" + i);
			outBytes[i] = valStrBytes[i - offset];
		}

		// to make it up 256 bytes
		for (int j = valStrBytes.length + offset; j < length + offset; j++) {
			// padding
			outBytes[j] = PADDING_CHAR;
		}
	}

	/**
	 * Create a common socket server to support normal server, repeater for both
	 * ft and cmd.
	 * 
	 * @param myPort
	 * @param dir
	 * @param cdMap
	 */
	private static void createServer(String myPort, String dir, Map<String, String> cdMap) {
		final String METHOD_NAME = "createServer(...)";

		int port = getPortNum(myPort);
		ServerSocket server = null;

		try {
			server = new ServerSocket(port);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}

		DataInputStream dis = null;
		DataOutputStream dos = null;
		FileOutputStream fos = null;

		// next server
		Socket client2 = null;
		DataInputStream dis2 = null;
		DataOutputStream dos2 = null;

		int port2 = 0;

		String sKeyServer = cdMap.get(CD_KEY_SKEY);

		while (true) {
			logInfo(METHOD_NAME, "Server listening on port=" + port + " with default dir=" + dir + " with secured mode=" + (isNotBlank(sKeyServer) ? "on" : "off"));
			String response = null;

			try {
				Socket socket = server.accept();

				String remoteIp = handleIpAddress(socket.getRemoteSocketAddress().toString(), IP_PORT_DELIMETER);
				String localIp = handleIpAddress(socket.getLocalSocketAddress().toString(), IP_PORT_DELIMETER);

				dis = new DataInputStream(socket.getInputStream());
				dos = new DataOutputStream(socket.getOutputStream());

				// read the first 256 bytes for control data
				byte[] controlDataBytes = new byte[CONTROL_DATA_BYTES];
				dis.read(controlDataBytes);

				Map<String, String> cdMapClient = new HashMap<String, String>();

				cdMapClient = updateMapFromControlDataBytes(controlDataBytes, cdMapClient);

				String fileName = cdMapClient.get(CD_KEY_FILE_NAME);
				logInfo(METHOD_NAME, "fileName=" + fileName);

				String dirDest = cdMapClient.get(CD_KEY_DIR_DEST);
				logInfo(METHOD_NAME, "dirDest=" + dirDest);

				String fileSize = cdMapClient.get(CD_KEY_FILE_SIZE);
				logInfo(METHOD_NAME, "fileSize=" + fileSize);

				String routerList = cdMapClient.get(CD_KEY_ROUTER_LIST);
				logInfo(METHOD_NAME, "routerList=" + routerList);

				String requestMode = cdMapClient.get(CD_KEY_REQUEST_MODE);
				logInfo(METHOD_NAME, "requestMode=" + requestMode);

				String cmd = cdMapClient.get(CD_KEY_CMD);
				logInfo(METHOD_NAME, "cmd=" + cmd);

				String sKey = cdMapClient.get(CD_KEY_SKEY);
				logInfo(METHOD_NAME, "sKey=" + sKey);

				logInfo(METHOD_NAME, "Request," + remoteIp + "=>" + localIp + ",cdMap=" + cdMapClient);

				// sKey verification required
				if (isNotBlank(sKeyServer)) {
					if (!sKeyServer.equals(sKey)) {
						response = RESPONSE_ACCESS_DENIED;
						dos.writeUTF(response);
						logInfo(METHOD_NAME, "Response," + localIp + "=>" + remoteIp + ",response=" + response);
						if (isNotBlank(fileSize)) {
							logInfo(METHOD_NAME, "trying to read all bytes from dis to make client happy.");
							// read the next fileSize bytes for the real file
							// content
							int fileSizeInt = Integer.parseInt(fileSize);
							byte[] fileSizeBuffer = new byte[fileSizeInt];
							logInfo(METHOD_NAME, "fileSizeBuffer.length=" + fileSizeBuffer.length);

							int bytesRead = dis.read(fileSizeBuffer);
							while (bytesRead < fileSizeInt) {
								bytesRead += dis.read(fileSizeBuffer, bytesRead, fileSizeInt - bytesRead);
							}

							logInfo(METHOD_NAME, "total bytesRead=" + bytesRead);
						}
						continue;
					}
				}

				String ip2 = null;
				String myPort2 = null;

				boolean viaRepeater = false;
				if (isNotBlank(routerList)) {
					viaRepeater = true;
					String[] ipPortStrList = routerList.split(ROUTER_ENTRY_DELIMETER);

					updateRouterList(cdMapClient, routerList);

					String[] ipPortStr = ipPortStrList[0].split(IP_PORT_DELIMETER);
					ip2 = ipPortStr[0];
					myPort2 = ipPortStr[1];
					port2 = getPortNum(myPort2);
					logInfo(METHOD_NAME, "ip2/myPort2/port2=" + ip2 + "/" + myPort2 + "/" + port2);

				}

				logInfo(METHOD_NAME, "viaRepeater/cdMap=" + viaRepeater + cdMapClient);
				if (viaRepeater) {

					// connect to next server
					client2 = new Socket(ip2, port2);
					String nextIp = handleIpAddress(client2.getRemoteSocketAddress().toString(), IP_PORT_DELIMETER);
					// String serverIp2 =
					// handleIpAddress(client2.getRemoteSocketAddress().toString(),
					// IP_PORT_DELIMETER);
					String localIp2 = handleIpAddress(client2.getLocalSocketAddress().toString(), IP_PORT_DELIMETER);
					dos2 = new DataOutputStream(client2.getOutputStream());
					dis2 = new DataInputStream(client2.getInputStream());

					// update the controlDataBytes
					controlDataBytes = updateControlDataBytes(controlDataBytes, cdMapClient);

					if (MODE_FT_CLIENT.equals(requestMode)) {

						logInfo(METHOD_NAME, "Request," + remoteIp + "=>[" + localIp + "]=>" + nextIp + ",fileName/fileSize=" + fileName + "/" + fileSize);

						int fileSizeInt = Integer.parseInt(fileSize);

						// send to server
						forwardFileToNextServer(dis, dos2, fileSizeInt, controlDataBytes);
						logInfo(METHOD_NAME, "Sent," + remoteIp + "=>[" + localIp + "]=>" + nextIp + ",fileName/fileSize=" + fileName + "/" + fileSize);

						logInfo(METHOD_NAME, "Waiting for response..." + nextIp + "=>" + localIp2);
						// read the response from server
						response = dis2.readUTF();

						response = appendServerInfo(response, localIp, nextIp, remoteIp, localIp2);
					} else if (MODE_CMD_CLIENT.equals(requestMode)) {
						logInfo(METHOD_NAME, "Request," + remoteIp + "=>[" + localIp + "]=>" + nextIp + ",cmd=" + cmd);

						// send to next server
						dos2.write(controlDataBytes);
						dos2.flush();
						logInfo(METHOD_NAME, "Sent," + remoteIp + "=>[" + localIp + "]=>" + nextIp + ",cmd=" + cmd);

						logInfo(METHOD_NAME, "Waiting for response..." + nextIp + "=>" + localIp2);
						// read the response from server
						response = dis2.readUTF();
						response = appendServerInfo(response, localIp, nextIp, remoteIp, localIp2);
					} else {
						logError(METHOD_NAME, "invalid requestMode=" + requestMode);
					}
				} else {

					if (MODE_FT_CLIENT.equals(requestMode)) {

						logInfo(METHOD_NAME, "Request," + remoteIp + "=>" + localIp + ",fileName/fileSize:dirDest=" + fileName + "/" + fileSize + "::" + dirDest);
						String dirFinal = isNotBlank(dirDest) ? dirDest : dir;
						String fileStr = dirFinal + "/" + fileName;
						fos = saveFileFromStream(dis, fileSize, fileStr);
						response = "Server stored file::size(bytes)=" + fileStr + "::" + fileSize + "\n";
						logInfo(METHOD_NAME, "Received," + response);
						response = appendServerInfo(response, localIp, localIp, remoteIp, null);
					} else if (MODE_CMD_CLIENT.equals(requestMode)) {
						logInfo(METHOD_NAME, "Request," + remoteIp + "=>" + localIp + ",cmd=" + cmd);

						// run the cmd on this server
						response = executeCmd(cmd);

						response = appendServerInfo(response, localIp, localIp, remoteIp, null);

					} else {
						logError(METHOD_NAME, "invalid requestMode=" + requestMode);
					}
				}

				dos.writeUTF(response);

				logInfo(METHOD_NAME, "Response," + localIp + "=>" + remoteIp + ",response=" + response);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				handleStreamClosing(dis);
				handleStreamClosing(dos);
				handleStreamClosing(fos);
				handleStreamClosing(dis2);
				handleStreamClosing(dos2);
				handleStreamClosing(client2);
			}
		}
	}

	private static Map<String, String> updateMapFromControlDataBytes(byte[] controlDataBytes, Map<String, String> cdMap) {
		final String METHOD_NAME = "updateMapFromControlDataBytes(...)";

		String controlData = new String(controlDataBytes);
		logInfo(METHOD_NAME, "controlData=" + controlData);
		controlData = controlData.replaceAll(Character.toString(PADDING_CHAR), "");
		logInfo(METHOD_NAME, "updated controlData=" + controlData);

		logInfo(METHOD_NAME, "before cdMap=" + cdMap);
		// reset the cdMap
		cdMap.clear();

		String[] cdArr = controlData.split(",");
		for (int i = 0; i < cdArr.length; i++) {
			String[] kvArr = cdArr[i].split("=");
			String key = kvArr[0];
			String val = kvArr[1];

			if (!isNotBlank(val)) {
				continue;
			}

			cdMap.put(key, val);
		}

		logInfo(METHOD_NAME, "after cdMap=" + cdMap);
		return cdMap;
	}

	private static byte[] updateControlDataBytes(byte[] cdBytes, Map<String, String> cdMap) {
		final String METHOD_NAME = "updateControlDataBytes(...)";

		// reset the cdBytes
		appendStringToBytesArray(cdBytes, "", 0, CONTROL_DATA_BYTES);

		StringBuffer sbf = new StringBuffer();

		for (Entry<String, String> item : cdMap.entrySet()) {
			if (!isNotBlank(item.getValue())) {
				continue;
			}

			if (sbf.length() > 0) {
				sbf.append(",");
			}

			sbf.append(item.getKey());
			sbf.append("=");
			sbf.append(item.getValue());
		}

		logInfo(METHOD_NAME, "cdMap/sbf=" + cdMap + "/" + sbf.toString());
		appendStringToBytesArray(cdBytes, sbf.toString(), 0, CONTROL_DATA_BYTES);

		return cdBytes;
	}

	/**
	 * Store the fileSize data as fileStr from dis stream.
	 * 
	 * @param dis
	 * @param fileSize
	 * @param fileStr
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static FileOutputStream saveFileFromStream(DataInputStream dis, String fileSize, String fileStr) throws IOException, FileNotFoundException {
		final String METHOD_NAME = "saveFileFromStream(...)";

		FileOutputStream fos;
		File outFile = createNewFile(fileStr, true);
		fos = new FileOutputStream(outFile);

		int fileSizeInt = Integer.parseInt(fileSize);
		// read the next fileSize bytes for the real file content
		byte[] fileSizeBuffer = new byte[fileSizeInt];

		int bytesRead = dis.read(fileSizeBuffer);
		while (bytesRead < fileSizeInt) {
			bytesRead += dis.read(fileSizeBuffer, bytesRead, fileSizeInt - bytesRead);
		}

		logInfo(METHOD_NAME, "total bytesRead=" + bytesRead);

		fos.write(fileSizeBuffer);

		logInfo(METHOD_NAME, "File wrote to: " + fileStr);
		return fos;
	}

	/**
	 * Handle/format the ip address.
	 * 
	 * @param ip
	 * @return
	 */
	public static String handleIpAddress(String ip, String portDelimeter) {
		final String METHOD_NAME = "handleIpAddress(...)";

		logInfo(METHOD_NAME, "ip=" + ip);
		ip = ip.replaceAll("localhost/", "").replaceAll("/", "").replaceAll(":", portDelimeter);
		logInfo(METHOD_NAME, "updated ip=" + ip);
		return ip;
	}

	/**
	 * Print the debug msg to the console.
	 * 
	 * @param msg
	 */
	public static void logDebug(String methodName, String msg) {
		if (LOG_LEVEL_DEBUG) {
			System.out.println(getLogPrefix(methodName, LOG_TYPE_DEBUG) + getMaskedStringForLogging(msg));
		}
	}

	/**
	 * Print the info msg to the console.
	 * 
	 * @param msg
	 */
	public static void logInfo(String methodName, String msg) {
		System.out.println(getLogPrefix(methodName, LOG_TYPE_INFO) + getMaskedStringForLogging(msg));
	}

	/**
	 * Print the error msg to the console.
	 * 
	 * @param msg
	 */
	public static void logError(String methodName, String msg) {
		System.err.println(getLogPrefix(methodName, LOG_TYPE_ERROR) + getMaskedStringForLogging(msg));
	}

	/**
	 * Print the warning msg to the console.
	 * 
	 * @param msg
	 */
	public static void logWarn(String methodName, String msg) {
		System.err.println(getLogPrefix(methodName, LOG_TYPE_WARN) + getMaskedStringForLogging(msg));
	}

	/**
	 * Return the log prefix for the logType.
	 * 
	 * @param logType
	 * @return
	 */
	public static String getLogPrefix(String methodName, String logType) {
		return getDateTimeStr(DATE_TIME_FORMAT_LOG) + "|" + logType + "|" + APP_NAME_SIMPLE + "|" + methodName + "|" + MODE + "|" + VERSION + "|";
	}

	/**
	 * Safely close a Closeable/Socket steam object.
	 * 
	 * @param streamObj
	 */
	public static void handleStreamClosing(Object streamObj) {
		final String METHOD_NAME = "handleStreamClosing(...)";

		if (streamObj != null) {

			if (streamObj instanceof Closeable) {

				try {
					((Closeable) streamObj).close();
					logInfo(METHOD_NAME, "Successfully close steam object (Closeable): " + streamObj);

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (streamObj instanceof Socket) {

				try {
					((Socket) streamObj).close();
					logInfo(METHOD_NAME, "Successfully close steam object (Socket): " + streamObj);

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				logError(METHOD_NAME, "not process as unknown type for steam object:" + streamObj);
			}

		} else {
			logError(METHOD_NAME, "not process as null steam object: " + streamObj);
		}
	}

	/**
	 * Return the current date time as string in format: DATE_TIME_FORMAT.
	 * 
	 * @return
	 */
	public static String getDateTimeStr(String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(new Date());
	}

	/**
	 * Append the server, ip info to the original response.
	 * 
	 * @param response
	 * @param localIp
	 * @param fromIp
	 * @param toIp
	 * @param viaIp
	 * @return
	 */
	private static String appendServerInfo(String response, String localIp, String fromIp, String toIp, String viaIp) {
		StringBuffer sbf = new StringBuffer();

		// logInfo(METHOD_NAME, "before response=" + response);

		sbf.append("{");
		sbf.append("server=" + localIp);
		sbf.append(",from=" + fromIp);
		sbf.append(",to=" + toIp);
		sbf.append(",via=" + viaIp);
		sbf.append(",res=");

		// append \n when it's not yet wrapped
		if (!response.contains("server=")) {
			sbf.append("\n");
		}

		sbf.append(response);

		sbf.append("}");

		// logInfo(METHOD_NAME, "after response=" + sbf.toString());
		return sbf.toString();
	}

	/**
	 * Create the target file including it's parent folders if not yet existed.
	 * Will backup the file when it's already existed when
	 * backupWhenAlreadyExist=true.
	 * 
	 * @param fileName
	 * @param backupWhenAlreadyExist
	 * @return
	 * @throws IOException
	 */
	public static File createNewFile(String fileName, boolean backupWhenAlreadyExist) throws IOException {
		final String METHOD_NAME = "createNewFile(...)";

		logInfo(METHOD_NAME, "FileName::backupWhenAlreadyExist=" + fileName + "::" + backupWhenAlreadyExist);
		// try create the file if not existed
		File file = new File(fileName);
		if (!file.exists()) {

			// create the folders if needed
			if (!file.getParentFile().exists()) {
				new File(file.getParent()).mkdirs();
				logInfo(METHOD_NAME, "New folder(s) created for: " + file.getParent());
			}

			if (file.createNewFile()) {
				logInfo(METHOD_NAME, "New file created: " + fileName);
			} else {
				logError(METHOD_NAME, "Fail to create new file: " + fileName);
				throw new IOException("failed to create new file: " + fileName);
			}
		} else {
			if (backupWhenAlreadyExist) {
				String fileBkkStr = fileName + "_" + getDateTimeStr(DATE_TIME_FORMAT_FILE);
				File fileBkk = new File(fileBkkStr);
				boolean status = file.renameTo(fileBkk);
				logInfo(METHOD_NAME, "File already existed, backup status::backupTo=" + status + "::" + fileBkkStr);

				file = new File(fileName);
			} else {
				logWarn(METHOD_NAME, "File already existed, and will be override");
			}
		}

		return file;
	}

	/**
	 * Backup the file by rename the original fileName.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static File renameFileIfExist(String fileName, String renamePrefix) throws IOException {
		final String METHOD_NAME = "renameFileIfExist(...)";

		logInfo(METHOD_NAME, "FileName=" + fileName);
		// try create the file if not existed
		File file = new File(fileName);
		if (file.exists()) {
			String fileBkkStr = fileName + "_" + renamePrefix + "_" + getDateTimeStr(DATE_TIME_FORMAT_FILE);
			File fileBkk = new File(fileBkkStr);
			boolean status = file.renameTo(fileBkk);
			logInfo(METHOD_NAME, "File already existed, backup status::backupTo=" + status + "::" + fileBkkStr);

			file = new File(fileName);
		}

		return file;
	}

	/**
	 * Execute the command on the target server (cdMap key routerList=format
	 * ip#port->ip2#port2...). The cmd (cdMap key cmd) could be a single cmd or
	 * a shell scripts on target server. And return the response from server.
	 * 
	 * @param cdMap
	 * @return
	 */
	private static String sendCmdToServer(Map<String, String> cdMap) {
		final String METHOD_NAME = "sendCmdToServer(...)";

		logInfo(METHOD_NAME, "cdMap=" + cdMap);
		String routerList = cdMap.get(CD_KEY_ROUTER_LIST);
		String cmd = cdMap.get(CD_KEY_CMD);

		String ip = null;
		String myPort = null;
		int port = 0;

		String response = null;

		if (isNotBlank(routerList)) {
			String[] ipPortStrList = routerList.split(ROUTER_ENTRY_DELIMETER);

			updateRouterList(cdMap, routerList);

			String[] ipPortStr = ipPortStrList[0].split(IP_PORT_DELIMETER);
			ip = ipPortStr[0];
			myPort = ipPortStr[1];
			port = getPortNum(myPort);
			logInfo(METHOD_NAME, "ip/myPort/port=" + ip + "/" + myPort + "/" + port);

		} else {
			logError(METHOD_NAME, "invalid routerList=" + routerList);
			response = "invalid routerList=" + routerList;

			logError(METHOD_NAME, "Response," + response);
			return response;
		}

		Socket client = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;

		try {

			client = new Socket(ip, port);

			String serverIp = handleIpAddress(client.getRemoteSocketAddress().toString(), IP_PORT_DELIMETER);
			String localIp = handleIpAddress(client.getLocalSocketAddress().toString(), IP_PORT_DELIMETER);

			logInfo(METHOD_NAME, "Request," + localIp + "=>" + serverIp + ",cmd=" + cmd);

			dos = new DataOutputStream(client.getOutputStream());

			byte[] cdBytes = new byte[CONTROL_DATA_BYTES];
			updateControlDataBytes(cdBytes, cdMap);
			dos.write(cdBytes);
			dos.flush();

			logInfo(METHOD_NAME, "Waiting for response..." + serverIp + "=>" + localIp);

			dis = new DataInputStream(client.getInputStream());
			response = dis.readUTF();

			logInfo(METHOD_NAME, "Response," + serverIp + "=>" + localIp + ",cmd_rs=" + response);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			handleStreamClosing(dos);
			handleStreamClosing(dis);
			handleStreamClosing(client);
		}

		return response;
	}

	private static void updateRouterList(Map<String, String> cdMap, String routerList) {
		final String METHOD_NAME = "updateRouterList(...)";
		String routerListNew = null;
		if (routerList.indexOf(ROUTER_ENTRY_DELIMETER) != -1) {
			// remove the "xxx#yyy->" from xxx#yyy->ddd#ccc...
			routerListNew = routerList.substring(routerList.indexOf(ROUTER_ENTRY_DELIMETER) + ROUTER_ENTRY_DELIMETER.length());
			cdMap.put(CD_KEY_ROUTER_LIST, routerListNew);
			logInfo(METHOD_NAME, "updated routerList/routerListNew=" + routerList + "/" + routerListNew);
		} else {
			// not via repeater
			cdMap.put(CD_KEY_ROUTER_LIST, null);
		}
	}

	/**
	 * Execute the cmd on this server, printing and return the cmd output.
	 * 
	 * @param cmd
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static String executeCmd(String cmd) {
		final String METHOD_NAME = "executeCmd(...)";

		String targetCmds = new String(cmd);
		Runtime rt = null;
		StringBuffer output = new StringBuffer();
		BufferedReader reader = null;

		try {
			rt = Runtime.getRuntime();
			rt.traceInstructions(true);

			logInfo(METHOD_NAME, "Executing, cmd=" + cmd);

			final Process process = rt.exec(targetCmds);
			ExecutorService es = Executors.newSingleThreadExecutor();
			Future<?> f = es.submit(new Runnable() {
				public void run() {
					try {
						logInfo(METHOD_NAME, "Waiting for return from process=" + process);
						process.waitFor();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});

			f.get(CMD_EXE_WAITING_TIME_SECONDS, TimeUnit.SECONDS);

			logInfo(METHOD_NAME, "Process completed, process=" + process);

			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = null;

			logInfo(METHOD_NAME, "Reading input stream, reader=" + reader);
			output.append("\n=====NORMAL-OUTPUT-START=====\n");
			while (reader.ready()) {
				line = reader.readLine();
				logDebug(METHOD_NAME, "Reading input stream, line=" + line);
				output.append(line + "\n");
			}
			output.append("\n=====NORMAL-OUTPUT-END=====\n");

			logInfo(METHOD_NAME, "Done read input stream, reader=" + reader);
			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			logInfo(METHOD_NAME, "Reading error stream, reader=" + reader);

			output.append("\n=====ERROR-OUTPUT-START=====\n");
			while (reader.ready()) {
				line = reader.readLine();
				logDebug(METHOD_NAME, "Reading error stream, line=" + line);
				output.append(line + "\n");
			}
			output.append("\n=====ERROR-OUTPUT-END=====\n");

			logInfo(METHOD_NAME, "Done read error stream, reader=" + reader);

			logInfo(METHOD_NAME, "Executed, cmd_rs=\n" + output);
		} catch (Throwable t) {
			t.printStackTrace();
			output.append("\n=====EXCEPTION-START=====\n");
			output.append(t);
			output.append("\n=====EXCEPTION-END=====\n");
			logError(METHOD_NAME, "Failed, cmd_rs=\n" + output);
		} finally {
			handleStreamClosing(reader);
		}

		return output.toString();
	}

	private static void forwardFileToNextServer(DataInputStream dis, DataOutputStream dos, int fileSize, byte[] controlDataBytes) throws IOException, FileNotFoundException {

		// send the contral data bytes
		dos.write(controlDataBytes);
		dos.flush();

		sendFileContentBytes(dis, dos, fileSize);
	}

	private static void sendFileContentBytes(DataInputStream dis, DataOutputStream dos, int fileSize) throws IOException {
		final String METHOD_NAME = "sendFileContentBytes(...)";

		// read the next fileSize bytes for the real file content
		byte[] fileSizeBuffer = new byte[fileSize];
		logInfo(METHOD_NAME, "fileSizeBuffer.length=" + fileSizeBuffer.length);

		int bytesRead = dis.read(fileSizeBuffer);
		while (bytesRead < fileSize) {
			bytesRead += dis.read(fileSizeBuffer, bytesRead, fileSize - bytesRead);
		}

		logInfo(METHOD_NAME, "total bytesRead=" + bytesRead);

		dos.write(fileSizeBuffer);
		dos.flush();

		logInfo(METHOD_NAME, "total forwarded fileSizeBuffer=" + fileSizeBuffer.length);
	}

	/**
	 * Get the string representation for the object array object: strArray.
	 * e.g.: return {[0]=Bill, [1]=XXX} for String[] strArr = new String[]
	 * {"Bill", "XXX"};
	 * 
	 * @param strArray
	 * @return
	 */
	public static String getStringForStringArray(Object[] strArray) {
		if (null == strArray) {
			return null;
		}

		StringBuffer sbf = new StringBuffer();
		sbf.append("{");
		for (int i = 0; i < strArray.length; i++) {
			if (sbf.length() > 1) {
				// 2 + elements
				sbf.append(", ");
			}
			sbf.append("[" + i + "]=" + strArray[i]);
		}
		sbf.append("}");

		return sbf.toString();
	}

	/**
	 * Check the string val, return true is it's not null and length > 0.
	 * 
	 * @param val
	 * @return
	 */
	public static boolean isNotBlank(String val) {
		return !(val == null || val.length() <= 0);
	}

	private static void checkResponse(String response) {
		if (response == null) {
			throw new RuntimeException(SocketUtilApp.RESPONSE_NULL);
		} else if (response.contains(SocketUtilApp.RESPONSE_ACCESS_DENIED)) {
			throw new RuntimeException(SocketUtilApp.RESPONSE_ACCESS_DENIED);
		} else {
			// normal response
		}
	}
}
