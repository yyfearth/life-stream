package server;

import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import data.ImageMessage;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class DistributedNode {
	static ClientBootstrap clientBootstrap;
	static ServerBootstrap serverBootstrap;

	public static void main(String[] args) {
		List<InetSocketAddress> serverAddressList = new ArrayList<InetSocketAddress>() {{
			this.add(new InetSocketAddress("localhost", 8080));
			this.add(new InetSocketAddress("localhost", 8081));
			this.add(new InetSocketAddress("localhost", 8082));
		}};

		configueBootstrap();

		listenAllPossiblePorts();
	}

	static void configueBootstrap() {
		ChannelFactory channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		serverBootstrap = new ServerBootstrap(channelFactory);

		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline channelPipeline = Channels.pipeline();

				// Decoders
				channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
				channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ImageMessage.ServerMessage.getDefaultInstance()));

				// Encoder
				channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
				channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

				channelPipeline.addLast("ServerHandler", new ServerHandler());

				return channelPipeline;
			}
		});

		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.keepAlive", true);
	}

	static void listenAllPossiblePorts() {
		int[] possiblePorts = {
				8080,
				8081,
				8082,
		};
		int portIndex = 0;
		String hostname = "localhost";
		int port = 0;

		do {
			try {
				port = possiblePorts[portIndex];
				serverBootstrap.bind(new InetSocketAddress(port));
			} catch (ChannelException ex) {
				System.out.println("Fail to listenAllPossiblePorts port: " + port);
				++portIndex;

				if (portIndex >= possiblePorts.length) {
					System.out.println("Fail to listenAllPossiblePorts all possible ports.");
					break;
				}

				continue;
			}

			System.out.println("Listening port: " + port);
			break;
		} while (true);
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
}

class NodeChannelHandler extends SimpleChannelHandler {

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

		ImageMessage.Image.Builder builder = ImageMessage.Image.newBuilder();
		ImageMessage.Image image = builder
				.setName("Test image")
				.setId(UUID.randomUUID().toString())
				.setNodeId(1)
				.setUserId(UUID.randomUUID().toString())
				.setMime(MediaType.ANY_IMAGE_TYPE.toString())
				.setLength(file.length())
				.setData(imageBytes)
				.setCreated(new Date().getTime())
				.setModified(new Date().getTime())
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