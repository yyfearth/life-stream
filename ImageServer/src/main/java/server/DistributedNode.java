package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedNode extends BasicThread {

	public static void main(String[] args) throws InterruptedException {
		DistributedNode distributedNode = new DistributedNode(0);
		Thread thread = new Thread(distributedNode);
		thread.start();

		System.out.println("Stop thread in 5 seconds.");
		Thread.sleep(5000);
		thread.interrupt();
	}

	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;
	ChannelFactory clientChannelFactory;
	ChannelFactory serverChannelFactory;

	Channel bindingChannel;
	Channel[] connectorChannels;

	List<ConnectionMonitor> connectionMonitorList = new ArrayList<>();
	List<InetSocketAddress> nodeList = new ArrayList<>();

	int nodeId = 0;

	public DistributedNode(int nodeId) {
		this.nodeId = nodeId;

		configueBootstrap();
	}

	void configueBootstrap() {

		Executor bossPool = Executors.newCachedThreadPool();
		Executor workerPool = Executors.newCachedThreadPool();
		clientChannelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		clientBootstrap = new ClientBootstrap(clientChannelFactory);
		clientBootstrap.setPipelineFactory(new DistributedNodePipelineFactory());
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);

		bossPool = Executors.newCachedThreadPool();
		workerPool = Executors.newCachedThreadPool();
		serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		serverBootstrap = new ServerBootstrap(serverChannelFactory);
		serverBootstrap.setPipelineFactory(new DistributedNodePipelineFactory());
		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);
	}

	void connect() {

		int bindingPort = 8000 + nodeId;
		System.out.println("Binding port " + bindingPort);
		bindingChannel = serverBootstrap.bind(new InetSocketAddress(bindingPort));

		InetSocketAddress[] otherNodeAddresses = generateOtherAddress(8000);

		connectorChannels = new Channel[otherNodeAddresses.length];

		for (int i = 0; i < otherNodeAddresses.length; i++) {
			InetSocketAddress inetSocketAddress = otherNodeAddresses[i];
			ChannelFuture channelFuture = clientBootstrap.connect(inetSocketAddress);
			channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

			channelFuture.awaitUninterruptibly();

			if (channelFuture.isSuccess() == false) {
				System.out.println("Fail to connect " + inetSocketAddress.toString());
				continue;
			}

			connectorChannels[i] = channelFuture.getChannel();
		}
	}

	void disconnect() {
	}

	public void stop() {
		isStopping = true;
	}

	InetSocketAddress[] generateOtherAddress(int basePort) {
		InetSocketAddress[] inetSocketAddresses = new InetSocketAddress[2];

		int neighborNodeId = 0;

		for (int i = 0; i < inetSocketAddresses.length + 1; i++) {
			if (i != nodeId) {
				inetSocketAddresses[i] = new InetSocketAddress("localhsot", basePort + i);
			}
		}

		return inetSocketAddresses;
	}

	Thread monitorThread;

	@Override
	public void run() {
		ConnectionMonitor connectionMonitor = new ConnectionMonitor(8080 + nodeId, generateOtherAddress(8080));

		monitorThread = new Thread(connectionMonitor);
		monitorThread.setPriority(Thread.MIN_PRIORITY);
		monitorThread.start();

		connect();

		while (isStopping == false) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}

		disconnect();

		connectionMonitor.stop();

		if (bindingChannel.isConnected()) {
			bindingChannel.close().awaitUninterruptibly();
		}

		for (Channel c : connectorChannels) {
			c.close().awaitUninterruptibly();
		}
	}
}

