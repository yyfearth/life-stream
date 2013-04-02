package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ConnectionMonitor implements Runnable {

	public boolean isStopping() {
		return isStopping;
	}

	public void setStopping(boolean stopping) {
		isStopping = stopping;
	}

	boolean isStopping = false;

	int listeningPort;
	ClientBootstrap clientBootstrap;
	ChannelFactory channelFactory;

	public ConnectionMonitor(int listeningPort) {
		this.listeningPort = listeningPort;

		configueBootstrap();
	}

	void configueBootstrap() {
		Executor bossPool = Executors.newCachedThreadPool();
		Executor workerPool = Executors.newCachedThreadPool();
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new MonitorPipelineFactory());
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);
	}

	@Override
	public void run() {

		try {
			System.out.println("Monitor is started");
			connnect(new InetSocketAddress("localhost", 8080));

			while (isStopping == false) {
				Thread.sleep(100);
			}

			disconnect();
			System.out.println("Monitor is stopped");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		isStopping = true;
	}

	Channel channelConnector ;

	public boolean connnect(InetSocketAddress inetSocketAddress) {
		ChannelFuture channelFuture = clientBootstrap.connect(inetSocketAddress);
		channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

		channelFuture.awaitUninterruptibly();

		if (channelFuture.isSuccess() == false) {
			channelFactory.releaseExternalResources();
		}

		channelConnector = channelFuture.getChannel();

		return channelConnector.isConnected();
	}

	public void disconnect() {

		if (channelConnector == null || channelConnector.isConnected() == false) {
			return;
		}

		channelConnector.close().awaitUninterruptibly();
		channelFactory.releaseExternalResources();
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