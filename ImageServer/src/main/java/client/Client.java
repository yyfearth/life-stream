package client;

import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import server.LifeStreamMessages;

import java.io.File;
import java.io.FileInputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Client {
	static ClientBootstrap clientBootstrap;

	public static void main(String[] args) {
		configueBootstrap();

		connnect();
	}

	private static void configueBootstrap() {
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline channelPipeline = Channels.pipeline();

				// Decoders
				channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
				channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(LifeStreamMessages.Image.getDefaultInstance()));

				// Encoder
				channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

				channelPipeline.addLast("ServerHandler", new ClientHandler());

				return channelPipeline;
			}
		});
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("keepAlive", true);
	}

	public static void connnect() {
		while (true) {
			ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(8080));
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println("Connect operation completed.");
				}
			});

			channelFuture.awaitUninterruptibly();

			if (channelFuture.isSuccess() == false) {
				Throwable cause = channelFuture.getCause();

				if (cause instanceof ConnectException) {
					int delayedSeconds = 1;
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
		System.out.println("Channel connected.");

		Channel channel = e.getChannel();

		File file = new File("Meow.jpg");

		if (file.exists() == false) {
			System.out.println("File doesn't exist");
			return;
		}

		FileInputStream fileInputStream = new FileInputStream(file);
		ByteString imageBytes = ByteString.readFrom(fileInputStream);

		long nowTimestamp = (new Date()).getTime();
		LifeStreamMessages.Image.Builder builder = LifeStreamMessages.Image.newBuilder();
		LifeStreamMessages.Image image = builder
				.setName("Test image")
				.setId(UUID.randomUUID().toString())
				.setMime(MediaType.ANY_IMAGE_TYPE.toString())
				.setLength(file.length())
				.setData(imageBytes)
				.setCreatedTimestamp(nowTimestamp)
				.setModifiedTimestamp(nowTimestamp)
				.build();

		fileInputStream.close();

		ChannelFuture channelFuture = channel.write(image);
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				System.out.println("Send operation is completed.");
				System.out.println("Successful: " + future.isSuccess());
				future.getChannel().close();
			}
		});
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();

		StringBuilder stringBuilder = new StringBuilder();

		while (channelBuffer.readable()) {
			stringBuilder.append((char) channelBuffer.readByte());
		}

		System.out.println("Message received from server: " + stringBuilder.toString());

		e.getChannel().close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Throwable cause = e.getCause();

		if (cause instanceof ConnectException) {
			return;
		}

		System.out.println("Unepected exception occured in client handler.");

		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}