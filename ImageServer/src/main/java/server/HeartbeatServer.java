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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HeartbeatServer extends BasicThread {

	public int getNodeId() {
		return nodeId;
	}

	public int getBindingPort() {
		return bindingPort;
	}

	public ServerBootstrap getServerBootstrap() {
		return serverBootstrap;
	}

	public ClientBootstrap getClientBootstrap() {
		return clientBootstrap;
	}

	int nodeId = -1;
	int bindingPort;
	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;

	ChannelFactory clientChannelFactory;
	ChannelFactory serverChannelFactory;

	Channel bindingChannel;
	NodeInfo[] nodeInfos;

	public int getNumConnections() {
		return heartbeatConnectionMap.size();
	}

	Map<Integer, HeartbeatConnection> heartbeatConnectionMap = new HashMap<>();

	public HeartbeatServer(int nodeId, int bindingPort, NodeInfo[] nodeInfos) {
		this.nodeId = nodeId;
		this.bindingPort = bindingPort;
		this.nodeInfos = nodeInfos;
	}

	long nextReconnectionTick = 0;
	final int reconnectDelaySeconds = 15;

	@Override
	public void run() {

		try {
			connnect();

			while (isStopping == false) {
				Thread.sleep(100);

				long nowTick = (new Date()).getTime();

				if (nowTick >= nextReconnectionTick) {
					reconnect();
					nextReconnectionTick += reconnectDelaySeconds * 1000;
				}
			}

			disconnect();
		} catch (InterruptedException | ChannelException ex) {
			ex.printStackTrace();
		}
	}

	void sendHeartBeat() {
		long nowTimesteamp = (new Date()).getTime();

		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			LifeStreamMessages.HeartBeatMessage.Builder builder = LifeStreamMessages.HeartBeatMessage.newBuilder();

			LifeStreamMessages.HeartBeatMessage heartBeatMessage = builder
					.setOperation(LifeStreamMessages.HeartBeatMessage.OperationType.PING)
					.setNodeId(this.nodeId)
					.setTimestamp(nowTimesteamp)
					.build();

			ChannelFuture channelFuture = heartbeatConnection.getChannel().write(heartBeatMessage);
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess() == false) {

					}
				}
			});
		}
	}

	void reconnect() {
		System.out.println("Reconnect...");
		// TODO: loop each channel which is not connected and try to reconnect.
	}

	void configueBootstrap() {
		Executor bossPool = Executors.newCachedThreadPool();
		Executor workerPool = Executors.newCachedThreadPool();
		clientChannelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		clientBootstrap = new ClientBootstrap(clientChannelFactory);
		clientBootstrap.setPipelineFactory(new MonitorPipelineFactory());
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);

		bossPool = Executors.newCachedThreadPool();
		workerPool = Executors.newCachedThreadPool();
		serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		serverBootstrap = new ServerBootstrap(serverChannelFactory);
		serverBootstrap.setPipelineFactory(new MonitorPipelineFactory());
		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);
	}

	public void stop() {
		isStopping = true;
	}

	public void connnect() {

		configueBootstrap();

		// Listen as a server.
		bindingChannel = serverBootstrap.bind(new InetSocketAddress(bindingPort));

		// Connect to other nodes.

		for (NodeInfo nodeInfo : nodeInfos) {
			HeartbeatConnection heartbeatConnection = new HeartbeatConnection(this, nodeInfo.getSocketAddress());
			heartbeatConnection.connect();
			heartbeatConnectionMap.put(nodeInfo.getNodeId(), heartbeatConnection);
		}
	}

	public void addNode(int nodeId, InetSocketAddress socketAddress) {
		HeartbeatConnection heartbeatConnection = new HeartbeatConnection(this, socketAddress);
		heartbeatConnectionMap.put(nodeId, heartbeatConnection);

		if (bindingChannel.isConnected()) {
			heartbeatConnection.connect();
		}
	}

	public boolean removeNode(int nodeId) {
		HeartbeatConnection heartbeatConnection = heartbeatConnectionMap.remove(nodeId);

		if (heartbeatConnection == null) {
			return false;
		}

		return heartbeatConnection.disconnect();
	}

	public void disconnect() {

		for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
			heartbeatConnection.disconnect();
		}

		long timeoutTick = (new Date()).getTime() + 10 * 1000;

		while (true) {

			long nowTick = (new Date()).getTime();

			if (nowTick >= timeoutTick) {
				break;
			}

			for (HeartbeatConnection heartbeatConnection : heartbeatConnectionMap.values()) {
				if (heartbeatConnection.isConnected()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		serverBootstrap.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();
	}
}

class MonitorPipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(LifeStreamMessages.HeartBeatMessage.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ServerHandler", new MonitorHandler());

		return channelPipeline;
	}
}