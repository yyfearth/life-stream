package server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
	public static void main(String[] args) {
		ChannelFactory channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		ServerBootstrap serverBootstrap = new ServerBootstrap(channelFactory);

		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline channelPipeline = Channels.pipeline();

				// Decoders
				channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
				channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ImageMessage.Image.getDefaultInstance()));

				// Encoder
				channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

				channelPipeline.addLast("ServerHandler", new ServerHandler());

				return channelPipeline;
			}
		});

		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);

		String hostname = "localhost";
		int port = 8080;
		serverBootstrap.bind(new InetSocketAddress(port));

		System.out.println("Start listening " + hostname + ":" + port);
	}
}

class ServerHandler extends SimpleChannelHandler {

	@Override
	public void channelConnected(ChannelHandlerContext channelHandlerContext, ChannelStateEvent e) throws Exception {
		System.out.println("Status: " + e.getState());
		System.out.println("Value: " + e.getValue());
		System.out.println("Name: " + channelHandlerContext.getName());

		Channel channel = e.getChannel();

		ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
		channelBuffer.writeBytes("Welcome to the Netty server. I am running on JVM, and it sucks!".getBytes());

		ChannelFuture channelFuture = channel.write(channelBuffer);

		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				System.out.println("Message sent.");
//				future.getChannel().close();
			}
		});
	}

	static int count = 0;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		ImageMessage.Image image = (ImageMessage.Image) e.getMessage();

		System.out.println(image.toString());

//		byte[] buffer = new byte[8192];
//
//		while (inChannelBuffer.readable()) {
//			System.out.println("Readable bytes: " + inChannelBuffer.readableBytes());
//
//			int numReadBytes = Math.min(buffer.length, inChannelBuffer.readableBytes());
//			inChannelBuffer.readBytes(buffer, 0, numReadBytes);
//			count += numReadBytes;
//		}
//
//		System.out.println(count + " bytes received");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		Throwable cause = e.getCause();

		if (cause instanceof IOException) {
			System.out.println(ctx.getChannel().getRemoteAddress() + " is disconnected");
			return;
		}

		System.out.println("Unepected exception occured in server handler.");

		cause.printStackTrace();
		e.getChannel().close();
	}
}