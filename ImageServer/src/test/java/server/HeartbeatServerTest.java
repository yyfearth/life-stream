package server;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class HeartbeatServerTest {

	List<HeartbeatServer> heartbeatServers = new ArrayList<>();
	List<Thread> threadList = new ArrayList<>();

	@Test(groups = {"Connect"})
	public void testConnect() throws Exception {
		HeartbeatServer heartbeatServer = new HeartbeatServer(new NodeInfo(0, 8090), new NodeInfo[]{
				new NodeInfo(1, 8091),
		});
		heartbeatServers.add(heartbeatServer);

		heartbeatServer = new HeartbeatServer(new NodeInfo(1, 8091), new NodeInfo[]{
				new NodeInfo(0, 8090),
		});
		heartbeatServers.add(heartbeatServer);

		for (HeartbeatServer server : heartbeatServers) {
			Thread thread = new Thread(server);
			thread.start();
			threadList.add(thread);
		}

		Thread.sleep(1000);

		for (HeartbeatServer server : heartbeatServers) {
			Assert.assertTrue(server.isBound());
		}
	}

	@Test(groups = {"AddNode"}, dependsOnGroups = {"Connect"})
	public void testAddNode() throws Exception {
		HeartbeatServer heartbeatServer2 = new HeartbeatServer(new NodeInfo(2, 8092), new NodeInfo[]{});
		heartbeatServers.add(heartbeatServer2);

		Thread thread = new Thread(heartbeatServer2);
		thread.start();
		threadList.add(thread);

		HeartbeatServer heartbeatServer0 = heartbeatServers.get(0);
		heartbeatServer0.addNode(heartbeatServer2.serverNodeInfo);

		heartbeatServer2.addNode(heartbeatServer0.serverNodeInfo);

		Assert.assertEquals(heartbeatServer0.getNumConnections(), 2);
		Assert.assertEquals(heartbeatServer2.getNumConnections(), 1);
	}

	@Test(groups = {"RemoveNode"}, dependsOnGroups = {"AddNode"})
	public void testRemoveNode() throws Exception {
		HeartbeatServer heartbeatServer0 = heartbeatServers.get(0);
		HeartbeatServer heartbeatServer1 = heartbeatServers.get(1);

		heartbeatServer0.removeNode(1);
		heartbeatServer1.removeNode(0);

		Assert.assertEquals(heartbeatServer0.getNumConnections(), 1);
		Assert.assertEquals(heartbeatServer1.getNumConnections(), 0);
	}

	@Test(groups = {"Event"}, dependsOnGroups = {"RemoveNode"}, priority = 100)
	public void testEvents() throws Exception {
		class CustomHeatBeatServerEventListener implements HeatBeatServerEventListener {
			Integer connectedNodeId;
			Integer disconnectedNodeId;

			@Override
			public void onConnected(Object caller, HeatBeatServerEventArgs eventArgs) {
				connectedNodeId = eventArgs.nodeInfo.nodeId;
			}

			@Override
			public void onDisconnected(Object caller, HeatBeatServerEventArgs eventArgs) {
				disconnectedNodeId = eventArgs.nodeInfo.nodeId;
			}

			@Override
			public void onClosed(Object caller, HeatBeatServerEventArgs eventArgs) {
			}
		}

		HeartbeatServer heartbeatServer0 = heartbeatServers.get(0);

		CustomHeatBeatServerEventListener listener = new CustomHeatBeatServerEventListener();
		heartbeatServer0.addEventListener(listener);

		HeartbeatServer heartbeatServer2 = heartbeatServers.get(2);
		heartbeatServer2.disconnect();

		Assert.assertNotNull(listener.disconnectedNodeId);
		Assert.assertEquals(listener.disconnectedNodeId.intValue(), 2);
	}

	@Test(groups = {"Disonnect"}, dependsOnGroups = {"RemoveNode"}, priority = 200)
	public void testDisconnect() throws Exception {
		class CustomHeatBeatServerEventListener implements HeatBeatServerEventListener {
			HeartbeatServer heartbeatServer;
			boolean isClosed = false;

			CustomHeatBeatServerEventListener(HeartbeatServer heartbeatServer) {
				this.heartbeatServer = heartbeatServer;
			}

			@Override
			public void onConnected(Object caller, HeatBeatServerEventArgs eventArgs) {
			}

			@Override
			public void onDisconnected(Object caller, HeatBeatServerEventArgs eventArgs) {
			}

			@Override
			public void onClosed(Object caller, HeatBeatServerEventArgs eventArgs) {
				isClosed = true;
				System.out.println(heartbeatServer.serverNodeInfo + " is closed.");
			}
		}

		List<CustomHeatBeatServerEventListener> customHeatBeatServerEventListenerList = new ArrayList<>();

		for (HeartbeatServer server : heartbeatServers) {
			CustomHeatBeatServerEventListener customHeatBeatServerEventListener = new CustomHeatBeatServerEventListener(server);
			customHeatBeatServerEventListenerList.add(customHeatBeatServerEventListener);
			server.addEventListener(customHeatBeatServerEventListener);
			server.stop();
		}

		while (customHeatBeatServerEventListenerList.size() > 0) {
			Thread.sleep(500);

			for (int i = 0; i < customHeatBeatServerEventListenerList.size(); ) {
				CustomHeatBeatServerEventListener listener = customHeatBeatServerEventListenerList.get(i);

				if (listener.isClosed) {
					customHeatBeatServerEventListenerList.remove(i);
				} else {
					System.out.println("Wait node" + listener.heartbeatServer.serverNodeInfo.nodeId + "...");
					i++;
				}
			}
		}
	}
}
