package client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Client {
	static ClientBootstrap clientBootstrap;

	public static void main(String[] args) {
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new ClientHandler());
			}
		});
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);

		connnect();
	}

	public static void connnect() {
		while (true) {
			ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(8080));
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println();
				}
			});

			channelFuture.awaitUninterruptibly();

			if (channelFuture.isSuccess() == false) {
				Throwable cause = channelFuture.getCause();

				if (cause instanceof ConnectException) {
					int delayedSeconds = 5;
					System.out.println("Connection failed. Retry after " + delayedSeconds + " seconds");

					try {
						Thread.sleep(delayedSeconds * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					continue;
				}

				cause.printStackTrace();
				break;
			}

			channelFuture.getChannel().getCloseFuture().awaitUninterruptibly();
			channelFuture.getChannel().getFactory().releaseExternalResources();
			break;
		}
	}

	public static void close() {
		clientBootstrap.getFactory().releaseExternalResources();
	}
}

class ClientHandler extends SimpleChannelHandler {

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();

		System.out.println("Connected to server");

		File file = new File("Meow.jpg");

		if (file.exists() == false) {
			System.out.println("File doesn't exist");
			return;
		}

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
			channelBuffer.writeBytes(fileInputStream, (int) file.length());
			fileInputStream.close();

			ChannelFuture channelFuture = channel.write(channelBuffer);
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println("The image has been sent.");
				}
			});
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();

		StringBuilder stringBuilder = new StringBuilder();

		while (channelBuffer.readable()) {
			stringBuilder.append((char)channelBuffer.readByte());
		}

		System.out.println("Message received from server: " + stringBuilder.toString());

		e.getChannel().close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.out.println("Unepected exception occured in client handler.");
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}