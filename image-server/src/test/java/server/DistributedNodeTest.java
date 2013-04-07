package server;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributedNodeTest {
	List<DistributedNode> distributedNodeList = new ArrayList<>();
	Map<DistributedNode, Thread> threadMap = new HashMap<>();

	@Test(groups = {"NodeConnect"})
	public void testConnect() throws Exception {
		DistributedNode node = new DistributedNode(new NodeInfo(0, 8080), new NodeInfo[]{});
		distributedNodeList.add(node);

		Thread thread = new Thread(node);
		threadMap.put(node, thread);
		thread.start();

		while (node.isBound() == false) {
			System.out.println("Wait server to start");
			Thread.sleep(500);
		}
	}

	@Test(groups = {"NodeEvent"}, dependsOnGroups = {"NodeConnect"}, priority = 100)
	public void testEvent() throws Exception {

	}

	@Test(groups = {"NodeDisconnect"}, dependsOnGroups = {"NodeConnect"}, priority = 200)
	public void testDisconnect() throws Exception {
		DistributedNode node0 = distributedNodeList.get(0);
		node0.stop();

		Thread thread0 = threadMap.get(node0);

		while (thread0.isAlive()) {
			System.out.println("Wait Node0 to stop");
			Thread.sleep(500);
		}
	}
}
