import org.testng.Assert;
import org.testng.annotations.Test;
import server.ConnectionMonitor;
import server.DistributedNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

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
		ConnectionMonitor[] connectionMonitor = new ConnectionMonitor[3];
		Thread[] threads = new Thread[connectionMonitor.length];

		InetSocketAddress[] listeningAddresses = new InetSocketAddress[connectionMonitor.length];
		for (int i = 0; i < listeningAddresses.length; i++) {
			listeningAddresses[i] = new InetSocketAddress(8080 + i);
		}

		for (int i = 0; i < connectionMonitor.length; i++) {
			connectionMonitor[i] = new ConnectionMonitor(8080 + i, listeningAddresses);

			Thread thread = new Thread(connectionMonitor[i], "Connection Monitor " + (i + 1));
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();

			threads[i] = thread;
		}

		System.out.println("Stop one thread each 5 seconds...");

		for (int i = 0; i < connectionMonitor.length; i++) {
			Thread.sleep(5000);
			connectionMonitor[i].stop();
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
}