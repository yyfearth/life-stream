package client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;

public class Client {
	public static void main(String[] args) {
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		ClientBootstrap clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new ClientHandler());
			}
		});

		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);
		ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(8080));
		channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
	}
}

class ClientHandler extends SimpleChannelHandler {

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		System.out.println("I have connected to " + channel.getRemoteAddress().toString());
	}

	int count = 1000;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();
		StringBuilder stringBuilder = new StringBuilder();

		while (channelBuffer.readable()) {
			stringBuilder.append((char) channelBuffer.readByte());
		}

		String message = stringBuilder.toString();

		System.out.println(new Date() + " Got message from server:" + message);

		Channel channel = e.getChannel();
		ChannelBuffer outChannelBuffer = ChannelBuffers.dynamicBuffer();

		if (outChannelBuffer.writable()) {
			if (count-- > 0) {
				System.out.println(new Date() + " Send: Ping");
				outChannelBuffer.writeBytes("Ping".getBytes());
			} else {
				channel.close();
			}
		}

		ChannelFuture channelFuture = channel.write(outChannelBuffer);
		channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

		Thread.sleep(1000);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}