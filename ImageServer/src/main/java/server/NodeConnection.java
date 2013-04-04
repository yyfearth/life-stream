package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NodeConnection extends Thread {

	///// Static methods
//	public static NodeConnection[] generateReports(InetSocketAddress[] inetSocketAddresses) {
//		NodeConnection[] nodeConnections = new NodeConnection[inetSocketAddresses.length];
//
//		for (int i = 0; i < nodeConnections.length; i++) {
//			nodeConnections[i] = new NodeConnection(inetSocketAddresses[i]);
//		}
//
//		return nodeConnections;
//	}

	///// Constructors

	public NodeConnection(ConnectionMonitor connectionMonitor, InetSocketAddress socketAddress) {
		this.connectionMonitor = connectionMonitor;
		this.socketAddress = socketAddress;
	}

	///// Data
	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	public void setSocketAddress(InetSocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	public boolean isConnected() {
		return isConnected.get();
	}

	public void isConnected(boolean isConnected) {
		this.isConnected.set(isConnected);
	}

	protected AtomicBoolean isConnected = new AtomicBoolean(false);

	ConnectionMonitor connectionMonitor;
	InetSocketAddress socketAddress;
	List<INodeEventHandler> monitorEventHandlerList = new ArrayList<>();

	public Channel getChannel() {
		return channel.get();
	}

	public void setChannel(Channel channel) {
		this.channel.set(channel);
	}

	AtomicReference<Channel> channel = new AtomicReference<>();

	///// Methods
	public void connect() {

		ClientBootstrap clientBootstrap = connectionMonitor.getClientBootstrap();
		clientBootstrap.setPipelineFactory(new NodeChannelPipelineFactory());

		ChannelFuture channelFuture = clientBootstrap.connect(this.socketAddress);
		channelFuture.addListener(new ConnectListener(this));
		setChannel(channelFuture.getChannel());
	}

	public void disconnect() {

		ChannelFuture channelFuture = getChannel().close();
		channelFuture.addListener(new DisconnectListener(this));
	}

	public void addDisconnectHandler(INodeEventHandler handler) {
		monitorEventHandlerList.add(handler);
	}

	public boolean removeDisconnectHandler(INodeEventHandler handler) {
		return monitorEventHandlerList.remove(handler);
	}
}

class NodeChannelPipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(LifeStreamMessages.HeartBeatMessage.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("Handler", new NodeChannelHandler());

		return channelPipeline;
	}
}

class DisconnectListener extends ConnectListener {

	DisconnectListener(NodeConnection nodeConnection) {
		super(nodeConnection);
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			nodeConnection.isConnected(false);
			nodeConnection.setChannel(null);
		} else {
			System.out.println("Cannot disconnect. Address: " + nodeConnection.getSocketAddress());
			future.getCause().printStackTrace();
		}
	}
}

class ConnectListener implements ChannelFutureListener {

	NodeConnection nodeConnection;

	ConnectListener(NodeConnection nodeConnection) {
		this.nodeConnection = nodeConnection;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			nodeConnection.isConnected(true);
			nodeConnection.setChannel(future.getChannel());
		} else {
			nodeConnection.isConnected(false);
			nodeConnection.setChannel(null);
		}
	}
}