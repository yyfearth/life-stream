package server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
	public static void main(String[] args) {
		ChannelFactory channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		ServerBootstrap serverBootstrap = new ServerBootstrap(channelFactory);

		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new ServerHandler());
			}
		});

		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);

		serverBootstrap.bind(new InetSocketAddress(8080));
		System.out.println("Start listening 8080");
	}
}

class ServerHandler extends SimpleChannelHandler {
	static int count = 0;

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		System.out.println("Status: " + e.getState());
		System.out.println("Name: " + ctx.getName());

		Channel channel = e.getChannel();

		ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
		channelBuffer.writeBytes("Welcome to the Netty server. I am running on JVM, and it sucks!".getBytes());

		ChannelFuture channelFuture = channel.write(channelBuffer);

		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
//				future.getChannel().close();
			}
		});
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Channel channel = e.getChannel();

		ChannelBuffer inChannelBuffer = (ChannelBuffer) e.getMessage();


		byte[] buffer = new byte[8192];
//		inChannelBuffer.readBytes(buffer);

		while (inChannelBuffer.readable()) {
			System.out.println("inChannelBuffer.readableBytes(): " + inChannelBuffer.readableBytes());

			int numReadBytes = Math.min(buffer.length, inChannelBuffer.readableBytes());
			inChannelBuffer.readBytes(buffer, 0, numReadBytes);
			count += numReadBytes;

			Thread.sleep(100);
		}

		System.out.println(count + " bytes received");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
		System.out.println("Channel closed");
	}
}