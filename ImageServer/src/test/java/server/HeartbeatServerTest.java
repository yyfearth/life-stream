package server;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class HeartbeatServerTest {

	List<HeartbeatServer> heartbeatServers = new ArrayList<>();

	@Test(groups = {"Connect"})
	public void testConnect() throws Exception {
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
	}

	@Test(groups = {"AddNode"}, dependsOnGroups = {"Connect"})
	public void testAddNode() throws Exception {

		HeartbeatServer heartbeatServer2 = new HeartbeatServer(2, 8092, new NodeInfo[]{});
		heartbeatServer2.connnect();
		heartbeatServers.add(heartbeatServer2);

		HeartbeatServer heartbeatServer0 = heartbeatServers.get(0);
		heartbeatServer0.addNode(2, new InetSocketAddress("localhost", 8092));

		heartbeatServer2.addNode(0, new InetSocketAddress("localhost", 8090));

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

	@Test(groups = {"Disonnect"}, dependsOnGroups = {"RemoveNode"})
	public void testDisconnect() throws Exception {
		for (HeartbeatServer cm : heartbeatServers) {
			cm.disconnect();
		}
	}
}
