package server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedNode extends BasicThread {

	public static void main(String[] args) throws InterruptedException {
		Thread[] threads = new Thread[3];
		DistributedNode[] distributedNodes = new DistributedNode[threads.length];

		for (int i = 0; i < threads.length; i++) {
			distributedNodes[i] = new DistributedNode(i);

			Thread thread = new Thread(distributedNodes[i]);
			thread.setName("Distribute node");
			thread.start();
		}

		System.out.println("After 5 seconds, stop one thread earch 5 seconds.");
		Thread.sleep(2000);

		for (int i = 0; i < threads.length; i++) {
			Thread.sleep(10000);
			distributedNodes[i].stop();
		}
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
		configueBootstrap();

		int bindingPort = 8090 + nodeId;
		bindingChannel = serverBootstrap.bind(new InetSocketAddress(bindingPort));

		System.out.println("Node " + nodeId + " is binding port " + bindingPort);

		InetSocketAddress[] otherNodeAddresses = generateOtherAddresses(8090);

		connectorChannels = new Channel[otherNodeAddresses.length];

		for (int i = 0; i < otherNodeAddresses.length; i++) {
			InetSocketAddress otherNodeAddress = otherNodeAddresses[i];
			ChannelFuture channelFuture = clientBootstrap.connect(otherNodeAddress);

			System.out.println("Node " + nodeId + " is connecting to " + otherNodeAddress);

			channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

			channelFuture.awaitUninterruptibly();

			if (channelFuture.isSuccess() == false) {
				System.out.println("Node " + nodeId + ": Fail to connect " + otherNodeAddress.toString());
				continue;
			}

			connectorChannels[i] = channelFuture.getChannel();
		}
	}

	void disconnect() {

		if (bindingChannel.isConnected()) {
			bindingChannel.close().awaitUninterruptibly();
		}

		for (Channel c : connectorChannels) {
			if (c != null && c.isConnected()) {
				c.close().awaitUninterruptibly();
			}
		}

		serverChannelFactory.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();
	}

	public void stop() {
		isStopping = true;
	}

	InetSocketAddress[] generateOtherAddresses(int basePort) {
		InetSocketAddress[] otherAddresses = new InetSocketAddress[2];

		int addressIndex = 0;

		for (int i = 0; i < otherAddresses.length + 1; i++) {
			if (i != nodeId) {
				otherAddresses[addressIndex++] = new InetSocketAddress("localhost", basePort + i);
			}
		}

		return otherAddresses;
	}

	Thread monitorThread;
	ConnectionMonitor connectionMonitor;

	@Override
	public void run() {
		connectionMonitor = new ConnectionMonitor(8080 + nodeId, generateOtherAddresses(8080));

		monitorThread = new Thread(connectionMonitor);
		monitorThread.setPriority(Thread.MIN_PRIORITY);
		monitorThread.start();

		connect();
		System.out.println("Node " + nodeId + " is connected");

		while (isStopping == false) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}

		if (connectionMonitor != null && connectionMonitor.isStopping == false) {
			connectionMonitor.stop();
		}

		disconnect();
		System.out.println("Node " + nodeId + " is disconnnected");
	}
}

