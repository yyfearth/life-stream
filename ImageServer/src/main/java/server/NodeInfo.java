package server;

import java.net.InetSocketAddress;

public class NodeInfo {
	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	public void setSocketAddress(InetSocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	int nodeId;
	InetSocketAddress socketAddress;

	public NodeInfo(int nodeId, InetSocketAddress socketAddress) {
		this.nodeId = nodeId;
		this.socketAddress = socketAddress;
	}
}