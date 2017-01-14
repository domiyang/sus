/*
- Description: 	This socket app unit test.
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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is the unit test impl for SocketUtilApp.
 * 
 * @author Domi Yang
 *
 */
public class SocketUtilAppTest {

	static String cmdClient = "javac -version";
	static String cmdClient2 = "hostname";

	static String serverPort = "1980";
	static String serverIp = "localhost";
	static String serverPort2 = "1983";
	static String serverIp2 = "localhost";
	static String serverPort3 = "1984";
	static String serverIp3 = "localhost";
	static String serverPort4 = "1985";
	static String serverIp4 = "localhost";

	static String test_dir_root = "./target/test_dir";
	
	static String sKeyFile = test_dir_root + "/my_key.txt";

	static String fileClient = test_dir_root + "/test1.txt";
	static String fileClient2 = test_dir_root + "/test2.txt";
	static String dirDest = test_dir_root + "/dirDest";
	static String dirServer = test_dir_root + "/dir_server";
	static String dirServer2 = test_dir_root + "/dir_server1";
	static String dirServer3 = test_dir_root + "/dir_server3";
	static String dirServer4 = test_dir_root + "/dir_server4";

	static boolean serverStarted = false;
	static boolean serverStarted2 = false;
	static boolean serverStarted3 = false;
	static boolean serverStarted4 = false;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// to setup the test folder structures and the test data file
		prepareTestDataFile(sKeyFile, "this contains the skey for secure connection.");
		prepareTestDataFile(fileClient, "this is a test content for f1");
		prepareTestDataFile(fileClient2, "this is a test content for f2");

	}

	private static void prepareTestDataFile(String fileNamePath, String fileContent) throws IOException {
		File file = SocketUtilApp.createNewFile(fileNamePath, false);
		Assert.assertTrue(file.canWrite());
		FileWriter fw = new FileWriter(file);
		fw.write(fileContent);
		fw.flush();
		SocketUtilApp.handleStreamClosing(fw);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File file = new File(test_dir_root);
		deleteWholeDirectory(file);
	}

	private static void deleteWholeDirectory(File file) {
		String METHOD_NAME = "deleteWholeDirectory(...)";
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] fileArr = file.listFiles();
				for (File subFile : fileArr) {
					deleteWholeDirectory(subFile);
				}
				
				boolean deleted = file.delete();
				String dirName = file.getName();
				SocketUtilApp.logInfo(METHOD_NAME, "dir/deleted=" + dirName + "/" + Boolean.toString(deleted));
				
			} else {
				boolean deleted = file.delete();
				String fileName = file.getName();
				SocketUtilApp.logInfo(METHOD_NAME, "file/deleted=" + fileName + "/" + Boolean.toString(deleted));
			}
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Run the runnable r in the background.
	 * 
	 * @param r
	 */
	private void runInBackground(Runnable r) {
		ExecutorService es = Executors.newSingleThreadExecutor();
		Future<?> f = es.submit(r);

		try {
			Object obj = f.get(2, TimeUnit.SECONDS);
			Assert.assertNull(obj);
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (TimeoutException e) {
			// e.printStackTrace();
			// do nothing here.
		}
	}

	private void shouldCreateServer(boolean withSKey) {
		final String METHOD_NAME = "shouldCreateServer()";

		handleSKeyProperty(withSKey);

		SocketUtilApp.logInfo(METHOD_NAME, "serverStarted=" + Boolean.toString(serverStarted));

		if (serverStarted) {
			SocketUtilApp.logWarn(METHOD_NAME, "not process as server already started.");
			return;
		}

		serverStarted = true;

		Runnable r = new Runnable() {
			public void run() {
				String[] args = new String[] { SocketUtilApp.MODE_SERVER, serverPort, dirServer };
				try {
					SocketUtilApp.startProcess(args);

				} catch (Throwable t) {
					t.printStackTrace();
					fail(t.getMessage());
				}
			}
		};

		runInBackground(r);
	}

	private void handleSKeyProperty(boolean withSKey) {
		if (withSKey) {
			System.setProperty(SocketUtilApp.SECRET_KEY_FILE, sKeyFile);
		} else {
			System.setProperty(SocketUtilApp.SECRET_KEY_FILE, "");
		}
	}

	@Test
	public void shouldCreateServer() {
		shouldCreateServer(false);
	}

	@Test
	public void shouldCreateServer2() {
		shouldCreateServer2(false);
	}

	@Test
	public void shouldCreateServer3() {
		shouldCreateServer3(true);
	}

	@Test
	public void shouldCreateServer4() {
		shouldCreateServer4(true);
	}

	private void shouldCreateServer2(boolean withSKey) {
		final String METHOD_NAME = "shouldCreateServer2()";

		handleSKeyProperty(withSKey);

		SocketUtilApp.logInfo(METHOD_NAME, "serverStarted2=" + Boolean.toString(serverStarted2));

		if (serverStarted2) {
			SocketUtilApp.logWarn(METHOD_NAME, "not process as server2 already started.");
			return;
		}

		serverStarted2 = true;

		Runnable r = new Runnable() {
			public void run() {
				String[] args = new String[] { SocketUtilApp.MODE_SERVER, serverPort2, dirServer2 };
				try {
					SocketUtilApp.startProcess(args);

				} catch (Throwable t) {
					t.printStackTrace();
					fail(t.getMessage());
				}
			}
		};

		runInBackground(r);
	}

	private void shouldCreateServer4(boolean withSKey) {
		final String METHOD_NAME = "shouldCreateServer4()";

		handleSKeyProperty(withSKey);

		SocketUtilApp.logInfo(METHOD_NAME, "serverStarted4=" + Boolean.toString(serverStarted4));

		if (serverStarted4) {
			SocketUtilApp.logWarn(METHOD_NAME, "not process as server4 already started.");
			return;
		}

		serverStarted4 = true;

		Runnable r = new Runnable() {
			public void run() {
				String[] args = new String[] { SocketUtilApp.MODE_SERVER, serverPort4, dirServer4 };
				try {
					SocketUtilApp.startProcess(args);

				} catch (Throwable t) {
					t.printStackTrace();
					fail(t.getMessage());
				}
			}
		};

		runInBackground(r);
	}

	private void shouldCreateServer3(boolean withSKey) {
		final String METHOD_NAME = "shouldCreateServer3()";

		handleSKeyProperty(withSKey);

		SocketUtilApp.logInfo(METHOD_NAME, "serverStarted3=" + Boolean.toString(serverStarted3));

		if (serverStarted3) {
			SocketUtilApp.logWarn(METHOD_NAME, "not process as server3 already started.");
			return;
		}

		serverStarted3 = true;

		Runnable r = new Runnable() {
			public void run() {
				String[] args = new String[] { SocketUtilApp.MODE_SERVER, serverPort3, dirServer3 };
				try {
					SocketUtilApp.startProcess(args);

				} catch (Throwable t) {
					t.printStackTrace();
					fail(t.getMessage());
				}
			}
		};

		runInBackground(r);
	}

	@Test
	public void shouldSuccessSecureSendFileToServer() {
		secureSendFileToServer(true);
	}

	@Test
	public void shouldFailSecureSendFileToServer() {
		secureSendFileToServer(false);
	}

	private void secureSendFileToServer(boolean clientWithSKey) {
		shouldCreateServer4(true);

		handleSKeyProperty(clientWithSKey);

		String testFile = fileClient;
		String targetFileFolder = dirServer4;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp4 + SocketUtilApp.IP_PORT_DELIMETER + serverPort4, testFile };
		sendFileToServerAndVerify(testFile, targetFileFolder, args, !clientWithSKey);
	}

	@Test
	public void shouldSendFileToServer() {

		shouldCreateServer(false);

		String testFile = fileClient;
		String targetFileFolder = dirServer;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort, testFile };
		sendFileToServerAndVerify(testFile, targetFileFolder, args);

	}

	@Test
	public void shouldSendFileToServerWithDirDest() {

		shouldCreateServer(false);

		String testFile = fileClient;
		String targetDirDest = dirDest;
		String targetFileFolder = targetDirDest != null ? targetDirDest : dirServer;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort, testFile, targetDirDest };
		sendFileToServerAndVerify(testFile, targetFileFolder, args);

	}

	@Test
	public void shouldFailSecureSendFileToServerViaRepeater() {
		secureFtClientViaRepeaterTest(false);
	}

	@Test
	public void shouldSecureSendFileToServerViaRepeater() {
		secureFtClientViaRepeaterTest(true);
	}

	private void secureFtClientViaRepeaterTest(boolean clientWithSKey) {
		shouldCreateServer3(false);
		shouldCreateServer4(true);

		handleSKeyProperty(clientWithSKey);

		String testFile = fileClient2;
		String targetFileFolder = dirServer4;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp3 + SocketUtilApp.IP_PORT_DELIMETER + serverPort3 + SocketUtilApp.ROUTER_ENTRY_DELIMETER
						+ serverIp4 + SocketUtilApp.IP_PORT_DELIMETER + serverPort4,
				testFile };
		sendFileToServerAndVerify(testFile, targetFileFolder, args, !clientWithSKey);
	}

	@Test
	public void shouldSendFileToServerViaRepeater() {

		shouldCreateServer(false);
		shouldCreateServer2(false);

		String testFile = fileClient2;
		String targetFileFolder = dirServer2;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort + SocketUtilApp.ROUTER_ENTRY_DELIMETER
						+ serverIp2 + SocketUtilApp.IP_PORT_DELIMETER + serverPort2,
				testFile };
		sendFileToServerAndVerify(testFile, targetFileFolder, args);
	}

	@Test
	public void shouldSendFileToServerViaRepeaterWithDirDest() {

		shouldCreateServer(false);
		shouldCreateServer2(false);

		String testFile = fileClient2;
		String targetDirDest = dirDest;
		String targetFileFolder = targetDirDest != null ? targetDirDest : dirServer;

		String[] args = new String[] { SocketUtilApp.MODE_FT_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort + SocketUtilApp.ROUTER_ENTRY_DELIMETER
						+ serverIp2 + SocketUtilApp.IP_PORT_DELIMETER + serverPort2,
				testFile, targetDirDest };
		sendFileToServerAndVerify(testFile, targetFileFolder, args);
	}

	private void sendFileToServerAndVerify(String testFile, String serverDir, String[] args) {
		sendFileToServerAndVerify(testFile, serverDir, args, false);
	}

	private void sendFileToServerAndVerify(String testFile, String serverDir, String[] args,
			boolean accessDeniedExpected) {
		final String METHOD_NAME = "sendFileToServerAndVerify(...)";

		String fileStrServer = null;
		File fileNewServer = null;

		String backupPrefix = "bkk";
		try {
			fileStrServer = serverDir + testFile.substring(testFile.lastIndexOf("/"));
			fileNewServer = SocketUtilApp.renameFileIfExist(fileStrServer, backupPrefix);
			// check the expected target file on server not existed yet
			Assert.assertEquals(true, !fileNewServer.exists());

			String response = SocketUtilApp.startProcess(args);

			SocketUtilApp.logInfo(METHOD_NAME, "response=" + response);

			if (accessDeniedExpected) {
				checkResponse(response, accessDeniedExpected);
				return;
			}

			File fileOri = new File(testFile);

			fileNewServer = new File(fileStrServer);
			// check the expected target file on server created
			Assert.assertEquals(true, fileNewServer.exists());

			Assert.assertEquals(fileOri.length(), fileNewServer.length());

		} catch (Throwable t) {
			t.printStackTrace();
			if (accessDeniedExpected) {
				checkResponse(t.getMessage(), accessDeniedExpected);
			} else {
				fail(t.getMessage());
			}
		}
	}

	private void checkResponse(String response, boolean accessDeniedExpected) {
		Assert.assertNotNull(response);

		if (accessDeniedExpected) {
			Assert.assertTrue(response.contains(SocketUtilApp.RESPONSE_ACCESS_DENIED));
		}
	}

	@Test
	public void shouldFailSecureSendCmdToServer() {
		secureCmdClientTest(false);
	}

	@Test
	public void shouldSecureSendCmdToServer() {
		secureCmdClientTest(true);
	}

	private void secureCmdClientTest(boolean clientWithSKey) {
		final String METHOD_NAME = "shouldFailSendCmdToServerAsCmdClient()";

		shouldCreateServer4(true);
		handleSKeyProperty(clientWithSKey);

		String[] args = new String[] { SocketUtilApp.MODE_CMD_CLIENT,
				serverIp4 + SocketUtilApp.IP_PORT_DELIMETER + serverPort4, cmdClient };
		try {
			String response = SocketUtilApp.startProcess(args);

			SocketUtilApp.logInfo(METHOD_NAME, "response=" + response);
			checkResponse(response, !clientWithSKey);
		} catch (Throwable t) {
			t.printStackTrace();
			if (!clientWithSKey) {
				checkResponse(t.getMessage(), !clientWithSKey);
			} else {
				fail(t.getMessage());
			}
		}
	}

	@Test
	public void shouldSendCmdToServer() {
		final String METHOD_NAME = "shouldSendCmdToServerAsCmdClient()";

		shouldCreateServer(false);

		String[] args = new String[] { SocketUtilApp.MODE_CMD_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort, cmdClient };
		String response = SocketUtilApp.startProcess(args);

		SocketUtilApp.logInfo(METHOD_NAME, "response=" + response);
		checkResponse(response, false);
	}

	@Test
	public void shouldFailSecureSendCmdToServerViaRepeater() {
		secureCmdRepeaterClientTest(false);
	}

	@Test
	public void shouldSecureSendCmdToServerViaRepeater() {
		secureCmdRepeaterClientTest(true);
	}

	private void secureCmdRepeaterClientTest(boolean clientWithSKey) {
		final String METHOD_NAME = "secureCmdRepeaterClientTest(...)";
		shouldCreateServer3(false);
		shouldCreateServer4(true);

		handleSKeyProperty(clientWithSKey);

		String[] args = new String[] { SocketUtilApp.MODE_CMD_CLIENT,
				serverIp3 + SocketUtilApp.IP_PORT_DELIMETER + serverPort3 + SocketUtilApp.ROUTER_ENTRY_DELIMETER
						+ serverIp4 + SocketUtilApp.IP_PORT_DELIMETER + serverPort4,
				cmdClient };
		try {
			String response = SocketUtilApp.startProcess(args);

			SocketUtilApp.logInfo(METHOD_NAME, "response=" + response);
			checkResponse(response, !clientWithSKey);
		} catch (Throwable t) {
			t.printStackTrace();
			if (!clientWithSKey) {
				checkResponse(t.getMessage(), !clientWithSKey);
			} else {
				fail(t.getMessage());
			}
		}
	}

	@Test
	public void shouldSendCmdToServerViaRepeater() {

		final String METHOD_NAME = "shouldSendCmdToServerAsCmdRepeaterClient(...)";

		shouldCreateServer(false);
		shouldCreateServer2(false);

		String[] args = new String[] { SocketUtilApp.MODE_CMD_CLIENT,
				serverIp + SocketUtilApp.IP_PORT_DELIMETER + serverPort + SocketUtilApp.ROUTER_ENTRY_DELIMETER
						+ serverIp2 + SocketUtilApp.IP_PORT_DELIMETER + serverPort2,
				cmdClient };
		String response = SocketUtilApp.startProcess(args);

		SocketUtilApp.logInfo(METHOD_NAME, "response=" + response);
		checkResponse(response, false);

	}

	@Test
	public void shouldPrintUsage() {

		String[] args = new String[] {};
		try {
			SocketUtilApp.startProcess(args);

		} catch (Throwable t) {
			t.printStackTrace();
			Assert.assertEquals("Please run with expected arguments.", t.getMessage());
		}
	}

}
