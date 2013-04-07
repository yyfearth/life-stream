import org.testng.Assert;
import org.testng.annotations.Test;
import server.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
			heartbeatServer[i] = new HeartbeatServer(new NodeInfo(i, 8080 + i), nodeInfos);

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
	public void testNodeConnection() throws Exception {
		List<HeartbeatServer> heartbeatServers = new ArrayList<>();

		HeartbeatServer heartbeatServer = new HeartbeatServer(new NodeInfo(0, 8090), new NodeInfo[]{
				new NodeInfo(1, new InetSocketAddress("localhost", 8091)),
		});
		heartbeatServers.add(heartbeatServer);

		heartbeatServer = new HeartbeatServer(new NodeInfo(1, 8091), new NodeInfo[]{
				new NodeInfo(0, 8090),
		});
		heartbeatServers.add(heartbeatServer);

		for (HeartbeatServer cm : heartbeatServers) {
			cm.run();
		}

//		HeartbeatConnection nodeConnection = new HeartbeatConnection(heartbeatServer, new InetSocketAddress("localhost", 8080));
//		nodeConnection.connect();
//
//		long timeoutTick = (new Date()).getTime() + 10 * 1000;
//
//		while (true) {
//			long nowTick = (new Date()).getTime();
//
//			if (nodeConnection.isBound()) {
//				break;
//			}
//
//			if (nowTick >= timeoutTick) {
//				Assert.fail("The node connection is timeout");
//				return;
//			}
//		}

		Thread.sleep(3000);

		for (HeartbeatServer server : heartbeatServers) {
			server.stop();
		}
	}

	/**
	 * This is a console application used to test the Heartbeat server.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		class CustomeHeatBeatServerEventListener implements HeatBeatServerEventListener {
			@Override
			public void onConnected(HeartbeatServer server, HeatBeatServerEventArgs eventArgs) {
				System.out.println("Node" + eventArgs.getNodeInfo().getNodeId() + " is conncted");
			}

			@Override
			public void onDisconnected(HeartbeatServer server, HeatBeatServerEventArgs eventArgs) {
				System.out.println("Node" + eventArgs.getNodeInfo().getNodeId() + " is disconnected");
			}

			@Override
			public void onClosed(HeartbeatServer server, HeatBeatServerEventArgs eventArgs) {
				System.out.println("Node" + eventArgs.getNodeInfo().getNodeId() + " is closed");
			}
		}

		Scanner scanner = new Scanner(System.in);

		HeartbeatServer heartbeatServer0 = new HeartbeatServer(new NodeInfo(0, 8090), new NodeInfo[]{
				new NodeInfo(1, 8091),
		});
		heartbeatServer0.addEventListener(new CustomeHeatBeatServerEventListener());
		new Thread(heartbeatServer0).start();

		System.out.println("Node0 is added");
		System.out.println("Press enter to add Node1");
		scanner.nextLine();

		HeartbeatServer heartbeatServer1 = new HeartbeatServer(new NodeInfo(1, 8091), new NodeInfo[]{
				new NodeInfo(0, 8090),
		});
		heartbeatServer1.addEventListener(new CustomeHeatBeatServerEventListener());
		new Thread(heartbeatServer1).start();

		System.out.println("Node1 is added");
		System.out.println("Press enter to add Node2");
		scanner.nextLine();

		HeartbeatServer heartbeatServer2 = new HeartbeatServer(new NodeInfo(2, 8092), new NodeInfo[]{
				new NodeInfo(0, 8090),
				new NodeInfo(1, 8091),
		});
		heartbeatServer2.addEventListener(new CustomeHeatBeatServerEventListener());
		new Thread(heartbeatServer2).start();

		System.out.println("Node2 is added");
		System.out.println("Press enter to kill Node1");
		scanner.nextLine();

		heartbeatServer1.stop();

		System.out.println("Node1 is killed");
	}
}