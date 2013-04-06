package server;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class HeartbeatServerTest {

	List<HeartbeatServer> heartbeatServers = new ArrayList<>();

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

		for (HeartbeatServer cm : heartbeatServers) {
			cm.connnect();
			Assert.assertTrue(cm.isBound());
		}

		Assert.assertTrue(heartbeatServer.isBound());
	}

	@Test(groups = {"AddNode"}, dependsOnGroups = {"Connect"})
	public void testAddNode() throws Exception {

		HeartbeatServer heartbeatServer2 = new HeartbeatServer(new NodeInfo(2, 8092), new NodeInfo[]{});
		heartbeatServer2.connnect();
		heartbeatServers.add(heartbeatServer2);

		HeartbeatServer heartbeatServer0 = heartbeatServers.get(0);
		heartbeatServer0.addNode(new NodeInfo(2, 8092));

		heartbeatServer2.addNode(new NodeInfo(0, 8090));

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
				connectedNodeId = eventArgs.nodeId;
			}

			@Override
			public void onDisconnected(Object caller, HeatBeatServerEventArgs eventArgs) {
				disconnectedNodeId = eventArgs.nodeId;
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
		for (HeartbeatServer cm : heartbeatServers) {
			cm.disconnect();
			Assert.assertFalse(cm.isBound());
		}
	}
}
