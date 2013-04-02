package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ConnectionMonitor extends BasicThread {

	int bindingPort;
	InetSocketAddress[] listeningAddresses;
	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;
	ChannelFactory clientChannelFactory;
	ChannelFactory serverChannelFactory;

	Channel bindingChannel;
	Channel[] connectorChannels;

	public ConnectionMonitor(int bindingPort, InetSocketAddress[] listeningAddresses) {
		this.bindingPort = bindingPort;
		this.listeningAddresses = listeningAddresses;
	}

	@Override
	public void run() {

		try {
			connnect();

			while (isStopping == false) {
				Thread.sleep(100);
			}

			disconnect();
		} catch (InterruptedException | ChannelException ex) {
			ex.printStackTrace();
		}
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

//		System.out.println("Binding " + bindingPort);
		bindingChannel = serverBootstrap.bind(new InetSocketAddress(bindingPort));
		connectorChannels = new Channel[listeningAddresses.length];

		for (int i = 0; i < listeningAddresses.length; i++) {
			InetSocketAddress inetSocketAddress = listeningAddresses[i];
			ChannelFuture channelFuture = clientBootstrap.connect(inetSocketAddress);
			channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

			channelFuture.awaitUninterruptibly();

			if (channelFuture.isSuccess() == false) {
				System.out.println("Fail to connect " + inetSocketAddress.toString());
			}

			connectorChannels[i] = channelFuture.getChannel();
		}
	}

	public void disconnect() {

		for (Channel channel : connectorChannels) {
			if (channel.isConnected()) {
				channel.close().awaitUninterruptibly();
			}
		}

		serverBootstrap.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();
	}
}

class MonitorPipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
//		ChannelPipeline channelPipeline = Channels.pipeline();
//
//		// Decoders
//		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
//		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ImageMessage.ServerMessage.getDefaultInstance()));
//
//		// Encoder
//		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
//		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());
//
//		channelPipeline.addLast("ServerHandler", new MonitorHandler());
//
//		return channelPipeline;
		return Channels.pipeline(new MonitorHandler());
	}
}