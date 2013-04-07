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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class DistributedNode extends BasicThread {
	final static Logger LOGGER = Logger.getLogger(DistributedNode.class.getName());

	public static void main(String[] args) throws InterruptedException {
//		new DistributedNode()
//		Thread thread = new Thread();
	}

	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;
	ChannelFactory clientChannelFactory;
	ChannelFactory serverChannelFactory;

	public boolean isBound() {
		return (serverChannel != null) && serverChannel.isBound();
	}

	Channel serverChannel;

	List<HeartbeatServer> heartbeatServerList = new ArrayList<>();
	List<InetSocketAddress> nodeList = new ArrayList<>();

	NodeInfo serverNodeInfo;
	Map<Integer, NodeConnection> nodeConnectionMap = new ConcurrentHashMap<>();

	public DistributedNode(NodeInfo serverNodeInfo, NodeInfo[] clientNodeInfos) {
		this.serverNodeInfo = serverNodeInfo;

		for (NodeInfo nodeInfo : clientNodeInfos) {
			NodeConnection nodeConnection = new NodeConnection(nodeInfo);
			nodeConnectionMap.put(nodeInfo.nodeId, nodeConnection);
		}
	}

	public void addNode(NodeInfo nodeInfo) {
		NodeConnection nodeConnection = new NodeConnection(nodeInfo);
		nodeConnectionMap.put(nodeInfo.nodeId, nodeConnection);

		if (isBound()) {
			heartbeatServer.addNode(nodeInfo);
			connectNode(nodeConnection);
		}
	}

	public boolean removeNode(int nodeId) {
		NodeConnection nodeConnection = nodeConnectionMap.remove(nodeId);

		if (nodeConnection == null) {
			return false;
		}

		heartbeatServer.removeNode(nodeId);

		if (nodeConnection.getChannel().isConnected()) {
			nodeConnection.getChannel().close();
		}

		return true;
	}

	void configueBootstrap() {
		Executor bossPool = Executors.newCachedThreadPool();
		Executor workerPool = Executors.newCachedThreadPool();
		serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		serverBootstrap = new ServerBootstrap(serverChannelFactory);
		serverBootstrap.setPipelineFactory(new NodeServerPipeLineFactory());
		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);

		bossPool = Executors.newCachedThreadPool();
		workerPool = Executors.newCachedThreadPool();
		clientChannelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		clientBootstrap = new ClientBootstrap(clientChannelFactory);
		clientBootstrap.setPipelineFactory(new NodeClientPipeLineFactory());
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);
	}

	void connect() {
		List<NodeInfo> nodeInfoList = new ArrayList<>();

		for (NodeConnection nodeConnection : nodeConnectionMap.values()) {
			int nodeId = nodeConnection.nodeInfo.nodeId;
			nodeInfoList.add(new NodeInfo(nodeId, nodeConnection.nodeInfo.socketAddress.getHostName(), 8090 + nodeId));
		}

		heartbeatServer = new HeartbeatServer(new NodeInfo(serverNodeInfo.nodeId, 8090 + serverNodeInfo.nodeId), nodeInfoList.toArray(new NodeInfo[nodeInfoList.size()]));

		heartbeatThread = new Thread(heartbeatServer);
		heartbeatThread.setName("Heartbeat Thread " + serverNodeInfo.nodeId);
		heartbeatThread.setPriority(Thread.MIN_PRIORITY);
		heartbeatThread.start();

		configueBootstrap();

		serverChannel = serverBootstrap.bind(serverNodeInfo.socketAddress);

		for (NodeConnection nodeConnection : nodeConnectionMap.values()) {
			connectNode(nodeConnection);
		}
	}

	void runHeartbeatServer() {

	}

	void connectNode(NodeConnection nodeConnection) {
		ChannelFuture channelFuture = clientBootstrap.connect(nodeConnection.nodeInfo.socketAddress);
		channelFuture.addListener(new NodeConnectChannelFutureListener(nodeConnection));
	}

	void disconnect() {
		heartbeatServer.stop();

		List<ChannelFuture> channelFutureList = new ArrayList<>();
		channelFutureList.add(serverChannel.close());

		for (NodeConnection connection : nodeConnectionMap.values()) {
			channelFutureList.add(connection.getChannel().close());
		}

		for (ChannelFuture future : channelFutureList) {
			future.awaitUninterruptibly();
		}

		serverChannelFactory.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();
	}

	public void stop() {
		isStopping = true;
	}

	Thread heartbeatThread;
	HeartbeatServer heartbeatServer;

	@Override
	public void run() {
		connect();
		LOGGER.info("Node" + serverNodeInfo.nodeId + " is connected");

		while (isStopping == false) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}

		if (heartbeatServer != null && heartbeatServer.isStopping == false) {
			heartbeatServer.stop();
		}

		disconnect();
		LOGGER.info("Node" + serverNodeInfo.nodeId + " is disconnnected");
	}
}

class NodeConnectChannelFutureListener implements ChannelFutureListener {
	NodeConnection nodeConnection;

	NodeConnectChannelFutureListener(NodeConnection nodeConnection) {
		this.nodeConnection = nodeConnection;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		Channel channel = future.getChannel();

		if (future.isSuccess()) {
			nodeConnection.setChannel(channel);
		}

		channel.close();
	}
}

class NodeConnection {

	NodeInfo nodeInfo;

	protected AtomicReference<Channel> channel = new AtomicReference<>();

	public NodeConnection(NodeInfo nodeInfo) {
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

class NodeServerPipeLineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ImageMessage.ImageRequest.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ServerHandler", new NodeServerChannelHandler());

		return channelPipeline;
	}
}

class NodeClientPipeLineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ImageMessage.ImageResponse.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ServerHandler", new NodeClientChannelHandler());

		return channelPipeline;
	}
}

// TODO
class NodeServerChannelHandler extends SimpleChannelHandler {
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
	}
}

// TODO
class NodeClientChannelHandler extends SimpleChannelHandler {
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
	}
}