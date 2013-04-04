import org.testng.Assert;
import org.testng.annotations.Test;
import server.HeartbeatServer;
import server.DistributedNode;
import server.NodeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AdHocTest {
	@Test
	public void testLoadingImage() throws Exception {

		File file = new File("Meow.jpg");

		if (file.exists() == false) {
			System.out.println("File doesn't exist");
			return;
		}

		FileInputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(file);

			int b;
			int count = 0;

			while ((b = fileInputStream.read()) != -1) {
				count++;
				System.out.print(Integer.toHexString(0x100 | b).substring(1).toUpperCase());

				if (count % 10 == 0) {
					System.out.println();
				}
			}

			System.out.println();

			Assert.assertEquals(count, file.length(), "The file size is not correct");

			fileInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testMultipleMonitors() throws Exception {
		HeartbeatServer[] heartbeatServer = new HeartbeatServer[3];
		Thread[] threads = new Thread[heartbeatServer.length];

		InetSocketAddress[] listeningAddresses = new InetSocketAddress[heartbeatServer.length];
		for (int i = 0; i < listeningAddresses.length; i++) {
			listeningAddresses[i] = new InetSocketAddress(8080 + i);
		}

		NodeInfo[] nodeInfos = new NodeInfo[2];

		for (int i = 0; i < heartbeatServer.length; i++) {
			heartbeatServer[i] = new HeartbeatServer(i, 8080 + i, nodeInfos);

			Thread thread = new Thread(heartbeatServer[i], "Connection Monitor " + (i + 1));
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();

			threads[i] = thread;
		}

		System.out.println("Stop one thread each 5 seconds...");

		for (int i = 0; i < heartbeatServer.length; i++) {
			Thread.sleep(5000);
			heartbeatServer[i].stop();
		}
	}

	@Test
	public void testMultipleNodes() throws Exception {

		Thread[] threads = new Thread[3];
		DistributedNode[] distributedNodes = new DistributedNode[threads.length];

		for (int i = 0; i < threads.length; i++) {
			distributedNodes[i] = new DistributedNode(i);

			Thread thread = new Thread(distributedNodes[i]);
			thread.setName("Distribute node");
			thread.start();
		}

		System.out.println("After 5 seconds, stop one thread earch 5 seconds.");
		Thread.sleep(2000);

		for (int i = 0; i < threads.length; i++) {
			distributedNodes[i].stop();
			Thread.sleep(5000);
		}
	}

	@Test
	public void testNodeConnection() throws Exception {
		List<HeartbeatServer> heartbeatServers = new ArrayList<>();

		HeartbeatServer heartbeatServer = new HeartbeatServer(0, 8090, new NodeInfo[]{
				new NodeInfo(1, new InetSocketAddress("localhost", 8091)),
		});
		heartbeatServers.add(heartbeatServer);

		heartbeatServer = new HeartbeatServer(1, 8091, new NodeInfo[]{
				new NodeInfo(0, new InetSocketAddress("localhost", 8090)),
		});
		heartbeatServers.add(heartbeatServer);

		for (HeartbeatServer cm : heartbeatServers) {
			cm.connnect();
		}

//		HeartbeatConnection nodeConnection = new HeartbeatConnection(heartbeatServer, new InetSocketAddress("localhost", 8080));
//		nodeConnection.connect();
//
//		long timeoutTick = (new Date()).getTime() + 10 * 1000;
//
//		while (true) {
//			long nowTick = (new Date()).getTime();
//
//			if (nodeConnection.isConnected()) {
//				break;
//			}
//
//			if (nowTick >= timeoutTick) {
//				Assert.fail("The node connection is timeout");
//				return;
//			}
//		}

		Thread.sleep(3000);

		for (HeartbeatServer cm : heartbeatServers) {
			cm.disconnect();
		}
	}
}