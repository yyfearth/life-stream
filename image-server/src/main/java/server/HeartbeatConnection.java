package server;

import org.jboss.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicReference;

public class HeartbeatConnection {

	NodeInfo nodeInfo;

	protected AtomicReference<Channel> channel = new AtomicReference<>();

	public HeartbeatConnection(NodeInfo nodeInfo) {
		this.nodeInfo = nodeInfo;
	}

	public Channel getChannel() {
		return channel.get();
	}

	public void setChannel(Channel channel) {
		this.channel.set(channel);
	}

	public boolean isConnected() {
		Channel channel = getChannel();
		return (channel != null) && channel.isConnected();
	}
}