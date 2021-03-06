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

	public NodeInfo(NodeInfo nodeInfo) {
		this(nodeInfo.nodeId, nodeInfo.socketAddress.getHostName(), nodeInfo.socketAddress.getPort());
	}

	public NodeInfo(int nodeId, InetSocketAddress socketAddress) {
		this.nodeId = nodeId;
		this.socketAddress = socketAddress;
	}

	public NodeInfo(int nodeId, String hostname, int port) {
		this.nodeId = nodeId;
		this.socketAddress = new InetSocketAddress(hostname, port);
	}

	public NodeInfo(int nodeId, int port) {
		this(nodeId, "localhost", port);
	}

	@Override
	public String toString() {
		return "Node:" + nodeId;
	}
}