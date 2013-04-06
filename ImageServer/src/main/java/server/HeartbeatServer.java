package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class HeartbeatServer extends BasicThread {
	final static Logger LOGGER = Logger.getLogger(HeartbeatServer.class.getName());

	public boolean isBound() {
		return (serverChannel != null) && serverChannel.isBound();
	}

	public NodeInfo getServerNodeInfo() {
		return new NodeInfo(serverNodeInfo);
	}

	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;

	ChannelFactory clientChannelFactory;
	ChannelFactory serverChannelFactory;

	Channel serverChannel;

	NodeInfo serverNodeInfo;
	Map<Integer, HeartbeatConnection> heartbeatConnectionMap = new ConcurrentHashMap<>();

	public HeartbeatServer(NodeInfo serverNodeInfo, NodeInfo[] clientNodeInfos) {
		this.serverNodeInfo = serverNodeInfo;

		for (NodeInfo nodeInfo : clientNodeInfos) {
			addNode(nodeInfo);
		}
	}

	public void addNode(NodeInfo nodeInfo) {
		HeartbeatConnection heartbeatConnection = new HeartbeatConnection(nodeInfo);
		heartbeatConnectionMap.put(nodeInfo.nodeId, heartbeatConnection);
	}

	public boolean removeNode(int nodeId) {
		HeartbeatConnection heartbeatConnection = heartbeatConnectionMap.remove(nodeId);

		return heartbeatConnection != null;
	}

	long nextReconnectionTick = 0;
	final int reconnectDelaySeconds = 15;

	@Override
	public void run() {
		try {
			connnect();

			while (isStopping == false) {
				Thread.sleep(3000);

				long nowTick = (new Date()).getTime();

				if (nowTick >= nextReconnectionTick) {
					sendHeartBeats();
					reconnect();
					triggerEvents();
					nextReconnectionTick += reconnectDelaySeconds * 1000;
				}
			}

			disconnect();
		} catch (InterruptedException | ChannelException ex) {
			ex.printStackTrace();
		}
	}

	void connnect() {
		LOGGER.info("Node" + serverNodeInfo.nodeId + " is connecting.");

		configueBootstrap();

		// Listen as a server.
		serverChannel = serverBootstrap.bind(serverNodeInfo.socketAddress);

		// Connect to other nodes.
		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			connectNode(heartbeatConnection.nodeInfo);
		}
	}

	void connectNode(NodeInfo nodeInfo) {
		ChannelFuture channelFuture = clientBootstrap.connect(nodeInfo.socketAddress);
		channelFuture.addListener(new ConnectCompleteHandler(nodeInfo));
	}

	void configueBootstrap() {
		Executor bossPool = Executors.newCachedThreadPool();
		Executor workerPool = Executors.newCachedThreadPool();
		clientChannelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		clientBootstrap = new ClientBootstrap(clientChannelFactory);
		clientBootstrap.setPipelineFactory(new HeartbeatClientPipelineFactory(this));
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);

		bossPool = Executors.newCachedThreadPool();
		workerPool = Executors.newCachedThreadPool();
		serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		serverBootstrap = new ServerBootstrap(serverChannelFactory);
		serverBootstrap.setPipelineFactory(new HeartbeatServerPipelineFactory(this));
		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);
	}

	void sendHeartBeats() {
		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			Channel channel = heartbeatConnection.getChannel();

			if (channel == null || channel.isConnected() == false) {
				continue;
			}

			HeartbeatMessage.HeartbeatRequest.Builder builder = HeartbeatMessage.HeartbeatRequest.newBuilder();
			HeartbeatMessage.HeartbeatRequest heartbeatRequest = builder
					.setNodeId(serverNodeInfo.nodeId)
					.setTimestamp((new Date()).getTime())
					.build();

			ChannelFuture channelFuture = channel.write(heartbeatRequest);
			channelFuture.addListener(new HeartbeatRequestHandler(heartbeatConnection));
		}
	}

	void reconnect() {
		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			if (heartbeatConnection.isConnected()) {
				continue;
			}

			connectNode(heartbeatConnection.nodeInfo);
		}
	}

	public int getNumConnections() {
		return heartbeatConnectionMap.size();
	}

	HeartbeatConnection findConnectionByChannel(Channel channel) {
		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			if (heartbeatConnection.getChannel() == channel) {
				return heartbeatConnection;
			}
		}

		return null;
	}

	class ConnectCompleteHandler implements ChannelFutureListener {
		NodeInfo nodeInfo;

		ConnectCompleteHandler(NodeInfo nodeInfo) {
			this.nodeInfo = nodeInfo;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				HeartbeatConnection heartbeatConnection = heartbeatConnectionMap.get(nodeInfo.nodeId);
				heartbeatConnection.setChannel(future.getChannel());

				eventQueue.add(new HeartbeatEvent(nodeInfo, HeartbeatEventType.Connect));
			}
		}
	}

	class HeartbeatRequestHandler implements ChannelFutureListener {
		HeartbeatConnection heartbeatConnection;

		HeartbeatRequestHandler(HeartbeatConnection heartbeatConnection) {
			this.heartbeatConnection = heartbeatConnection;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				return;
			}

			heartbeatConnection.getChannel().disconnect();
			eventQueue.add(new HeartbeatEvent(heartbeatConnection.nodeInfo, HeartbeatEventType.Disconnect));
		}
	}

	NodeInfo findNodeInfoByChannel(Channel channel) {
		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			if (heartbeatConnection.getChannel() == channel) {
				return heartbeatConnection.nodeInfo;
			}
		}

		return null;
	}

	void disconnect() {
		LOGGER.info("Node" + serverNodeInfo.nodeId + " is disconnecting.");

		List<ChannelFuture> channelFutureList = new ArrayList<>();
		channelFutureList.add(serverChannel.close());

		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			Channel channel = heartbeatConnection.getChannel();

			if (channel != null && channel.isConnected()) {
				channelFutureList.add(channel.close());
			}
		}

		// Wait for all asynchronous calls to finish.
		for (ChannelFuture channelFuture : channelFutureList) {
			channelFuture.awaitUninterruptibly();
		}

		// And then we could explode the factory. Oh yeah.
		serverChannelFactory.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();

		for (HeatBeatServerEventListener listener : listenerList) {
			listener.onClosed(this, new HeatBeatServerEventArgs(serverNodeInfo));
		}
	}

	/*
	 * Listener
	 */

	Queue<HeartbeatEvent> eventQueue = new ConcurrentLinkedDeque<>();
	List<HeatBeatServerEventListener> listenerList = new ArrayList<>();

	public void addEventListener(HeatBeatServerEventListener listener) {
		listenerList.add(listener);
	}

	public boolean removeEventListener(HeatBeatServerEventListener listener) {
		return listenerList.remove(listener);
	}

	void triggerEvents() {
		while (eventQueue.isEmpty() == false) {
			HeartbeatEvent event = eventQueue.remove();

			for (HeatBeatServerEventListener listener : listenerList) {
				if (event.type == HeartbeatEventType.Connect) {
					listener.onConnected(this, new HeatBeatServerEventArgs(event.nodeInfo));
				} else if (event.type == HeartbeatEventType.Disconnect) {
					listener.onDisconnected(this, new HeatBeatServerEventArgs(event.nodeInfo));
				} else if (event.type == HeartbeatEventType.Close) {
					listener.onClosed(this, new HeatBeatServerEventArgs(event.nodeInfo));
				}
			}
		}
	}
}

enum HeartbeatEventType {
	Connect,
	Disconnect,
	Close,
}

class HeartbeatEvent {
	NodeInfo nodeInfo;
	HeartbeatEventType type;

	HeartbeatEvent(NodeInfo nodeInfo, HeartbeatEventType type) {
		this.nodeInfo = nodeInfo;
		this.type = type;
	}
}

class HeartbeatServerPipelineFactory implements ChannelPipelineFactory {
	HeartbeatServer heartbeatServer;

	HeartbeatServerPipelineFactory(HeartbeatServer heartbeatServer) {
		this.heartbeatServer = heartbeatServer;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(HeartbeatMessage.HeartbeatRequest.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ServerHandler", new ServerChannelHandler(heartbeatServer));

		return channelPipeline;
	}
}

class HeartbeatClientPipelineFactory implements ChannelPipelineFactory {
	HeartbeatServer heartbeatServer;

	HeartbeatClientPipelineFactory(HeartbeatServer heartbeatServer) {
		this.heartbeatServer = heartbeatServer;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(HeartbeatMessage.HeartbeatResponse.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ClientHandler", new ClientChannelHandler(heartbeatServer));

		return channelPipeline;
	}
}

class ServerChannelHandler extends SimpleChannelHandler {
	HeartbeatServer heartbeatServer;

	ServerChannelHandler(HeartbeatServer heartbeatServer) {
		this.heartbeatServer = heartbeatServer;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		HeartbeatMessage.HeartbeatRequest request = (HeartbeatMessage.HeartbeatRequest) e.getMessage();
//		System.out.println("Node" + heartbeatServer.serverNodeInfo.nodeId + " receives request " + request);

		HeartbeatMessage.HeartbeatResponse.Builder builder = HeartbeatMessage.HeartbeatResponse.newBuilder();
		HeartbeatMessage.HeartbeatResponse heartbeatResponse = builder
				.setNodeId(heartbeatServer.serverNodeInfo.nodeId)
				.setTimestamp((new Date()).getTime())
				.build();

//		System.out.println("Node" + heartbeatServer.serverNodeInfo.nodeId + " sends " + heartbeatResponse);

		e.getChannel().write(heartbeatResponse);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
	}
}

class ClientChannelHandler extends SimpleChannelHandler {
	HeartbeatServer heartbeatServer;

	ClientChannelHandler(HeartbeatServer heartbeatServer) {
		this.heartbeatServer = heartbeatServer;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		HeartbeatMessage.HeartbeatResponse response = (HeartbeatMessage.HeartbeatResponse) e.getMessage();
//		System.out.println("Node" + heartbeatServer.serverNodeInfo.nodeId + " receives response " + response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		if (e.getCause() instanceof ConnectException) {
			Channel channel = e.getChannel();
			HeartbeatConnection targetHeartbeatConnection = heartbeatServer.findConnectionByChannel(channel);

			if (targetHeartbeatConnection == null) {
				return;
			}

			heartbeatServer.eventQueue.add(new HeartbeatEvent(targetHeartbeatConnection.nodeInfo, HeartbeatEventType.Disconnect));

			if (e.getChannel().isConnected()) {
				e.getChannel().close();
			}
		} else {
			e.getCause().printStackTrace();
		}
	}


	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		HeartbeatConnection targetHeartbeatConnection = heartbeatServer.findConnectionByChannel(channel);
		heartbeatServer.eventQueue.add(new HeartbeatEvent(targetHeartbeatConnection.nodeInfo, HeartbeatEventType.Connect));
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		HeartbeatConnection heartbeatConnection = heartbeatServer.findConnectionByChannel(channel);

		if (heartbeatConnection == null) {
			System.out.println("Cannot determine the connection");
			return;
		}

		HeartbeatEvent heartbeatEvent = new HeartbeatEvent(heartbeatConnection.nodeInfo, HeartbeatEventType.Disconnect);
		heartbeatServer.eventQueue.add(heartbeatEvent);
	}
}

/**
 * This is a channel handler simply print the method name it reaches. It's for debugging purpose.
 */
class LiteralChannelHandler extends SimpleChannelHandler {

	final static Logger LOGGER = Logger.getLogger(HeartbeatServer.class.getName());

	NodeInfo serverNodeInfo;

	LiteralChannelHandler(NodeInfo serverNodeInfo) {
		this.serverNodeInfo = serverNodeInfo;
	}

	void logEvent(ChannelEvent e) {
		final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		Channel channel = e.getChannel();
		LOGGER.info(channel.getLocalAddress() + " <-> " + channel.getRemoteAddress() + ":" + stackTraceElements[2].getMethodName());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		logEvent(e);
		super.messageReceived(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		logEvent(e);
		super.exceptionCaught(ctx, e);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logEvent(e);
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logEvent(e);
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
		logEvent(e);
		super.writeComplete(ctx, e);
	}
}