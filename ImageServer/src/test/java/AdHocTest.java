import org.testng.Assert;
import org.testng.annotations.Test;
import server.ConnectionMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
	public void testThread() throws Exception {
		ConnectionMonitor[] connectionMonitor = new ConnectionMonitor[3];
		Thread[] threads = new Thread[connectionMonitor.length];

		for (int i = 0; i < connectionMonitor.length; i++) {
			connectionMonitor[i] = new ConnectionMonitor(8080 + i);

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
}

//class MockedMonitor extends ConnectionMonitor {
//	@Override
//	public void run() {
//
//		try {
//			System.out.println("Monitor is started");
//			connnect(new InetSocketAddress("localhost", 8080));
//
//			while (isStopping() == false) {
//				Thread.sleep(100);
//			}
//
//			disconnect();
//			System.out.println("Monitor is stopped");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
//}
//
//class MockedMonitorHandler extends MonitorHandler {
//	@Override
//	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//		super.messageReceived(ctx, e);
//	}
//}