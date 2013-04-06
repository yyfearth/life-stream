package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HeartbeatConnection {

	AtomicBoolean isStopping = new AtomicBoolean(false);

	///// Constructors

	public HeartbeatConnection(HeartbeatServer heartbeatServer, NodeInfo nodeInfo) {
		this.heartbeatServer = heartbeatServer;
		this.nodeInfo = nodeInfo;
	}

	///// Data

	public boolean isConnected() {
		return isConnected.get();
	}

	protected void isConnected(boolean isConnected) {
		this.isConnected.set(isConnected);
	}

	protected AtomicBoolean isConnected = new AtomicBoolean(false);

	HeartbeatServer heartbeatServer;
	NodeInfo nodeInfo;
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

		ClientBootstrap clientBootstrap = heartbeatServer.getClientBootstrap();
		clientBootstrap.setPipelineFactory(new HeartbeatPipelineFactory(this));

		ChannelFuture channelFuture = clientBootstrap.connect(nodeInfo.socketAddress);
		channelFuture.addListener(new ConnectListener(this));
		setChannel(channelFuture.getChannel());
	}

	public boolean disconnect() {
		Channel channel = getChannel();

		if (channel != null) {
			ChannelFuture channelFuture = getChannel().close();
			channelFuture.addListener(new DisconnectListener(this));

			return true;
		}

		return false;
	}

	public void addDisconnectHandler(INodeEventHandler handler) {
		monitorEventHandlerList.add(handler);
	}

	public boolean removeDisconnectHandler(INodeEventHandler handler) {
		return monitorEventHandlerList.remove(handler);
	}
}

class HeartbeatPipelineFactory implements ChannelPipelineFactory {

	HeartbeatPipelineFactory(HeartbeatConnection heartbeatConnection) {
		this.heartbeatConnection = heartbeatConnection;
	}

	HeartbeatConnection heartbeatConnection;

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(LifeStreamMessages.HeartBeatMessage.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("Handler", new HeartbeatClientChannelHandler(heartbeatConnection));

		return channelPipeline;
	}
}

class HeartbeatClientChannelHandler extends SimpleChannelHandler {

	HeartbeatClientChannelHandler(HeartbeatConnection heartbeatConnection) {
		this.heartbeatConnection = heartbeatConnection;
	}

	HeartbeatConnection heartbeatConnection;

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		System.out.println(channel.getLocalAddress() + " has connected to " + channel.getRemoteAddress());
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		System.out.println(channel.getLocalAddress() + " has disconnected to " + channel.getRemoteAddress());
	}
}

class DisconnectListener extends ConnectListener {

	DisconnectListener(HeartbeatConnection heartbeatConnection) {
		super(heartbeatConnection);
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			heartbeatConnection.isConnected(false);
			heartbeatConnection.setChannel(null);
		} else {
			System.out.println("Cannot disconnect. Address: " + heartbeatConnection.nodeInfo.socketAddress);
			future.getCause().printStackTrace();
		}
	}
}

class ConnectListener implements ChannelFutureListener {

	HeartbeatConnection heartbeatConnection;

	ConnectListener(HeartbeatConnection heartbeatConnection) {
		this.heartbeatConnection = heartbeatConnection;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			heartbeatConnection.isConnected(true);
			heartbeatConnection.setChannel(future.getChannel());
		} else {
			heartbeatConnection.isConnected(false);
			heartbeatConnection.setChannel(null);
		}
	}
}