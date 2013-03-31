package client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
		ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(8080));
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess() == false) {
					System.out.println("Connection failed, retry after 5 seconds.");
					Thread.sleep(5000);

					System.out.println("Retrying");
					connnect();
				}
			}
		});
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
					System.out.println("The image has been sent");
				}
			});
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
		System.out.println("Channel closed");
	}
}