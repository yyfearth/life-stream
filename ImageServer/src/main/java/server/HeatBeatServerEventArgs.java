package server;

public class HeatBeatServerEventArgs {
	public NodeInfo getNodeInfo() {
		return nodeInfo;
	}

	NodeInfo nodeInfo;

	public HeatBeatServerEventArgs(NodeInfo nodeInfo) {
		this.nodeInfo = nodeInfo;
	}
}
