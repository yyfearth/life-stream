package lifestream.user.client;

import lifestream.user.bean.UserEntity;
import lifestream.user.data.UserMessage;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class UserClient {
	private static final Logger logger = Logger.getLogger(UserClient.class.getSimpleName());

	protected final String host;
	protected final int port;
	protected final ClientBootstrap bootstrap;
	protected final UserClientHandler clientHandler;

	public UserClient(String host, int port) {
		this.host = host;
		this.port = port;

		// Set up
		bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Configure the event pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline p = Channels.pipeline();
				p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
				p.addLast("protobufDecoder", new ProtobufDecoder(UserMessage.Response.getDefaultInstance()));

				p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
				p.addLast("protobufEncoder", new ProtobufEncoder());

				p.addLast("handler", clientHandler);
				return p;
			}
		});

		clientHandler = new UserClientHandler() {
			@Override
			public void received(UserMessage.Response response) {
				logger.info("Received: " + response.toString());
				if (response.getResult() == UserMessage.Response.ResultCode.OK) {
					if (response.getRequest() != UserMessage.RequestType.PING) {
						receivedUser(UUID.fromString(response.getId()), response.getUser());
					} // ignore pong for now
				} else {
					receivedError(UUID.fromString(response.getId()), response.getResult(), response.getMessage());
				}
			}
		};
	}

	@Override
	public void finalize() throws Throwable {
		// Shut down all thread pools to exit.
		bootstrap.releaseExternalResources();

		super.finalize();
	}

	public UUID ping() {
		return clientHandler.request();
	}

	public UUID getUser(UserEntity user) {
		return getUser(user.getId());
	}

	public UUID getUser(UUID userId) {
		return requestUser(UserMessage.RequestType.GET_USER, userId);
	}

	public UUID addUser(UserEntity user) {
		return requestUser(UserMessage.RequestType.ADD_USER, user);
	}

	public UUID updateUser(UserEntity user) {
		return requestUser(UserMessage.RequestType.UPDATE_USER, user);
	}

	public UUID removeUser(UserEntity user) {
		return removeUser(user.getId());
	}

	public UUID removeUser(UUID userId) {
		return requestUser(UserMessage.RequestType.UPDATE_USER, userId);
	}

	public UUID requestUser(UserMessage.RequestType type, UserEntity user) {
		return clientHandler.request(type, user);
	}

	public UUID requestUser(UserMessage.RequestType type, UUID userId) {
		return clientHandler.request(type, userId);
	}

	private void receivedUser(UUID requestId, UserMessage.User user) {
		receivedUser(requestId, user == null ? null : new UserEntity(user));
	}

	// should be override
	public void receivedUser(UUID requestId, UserEntity user) {
		logger.info("Response - Success: " + requestId + "\n" + user + "\n");
	}

	// should be override
	public void receivedError(UUID requestId, UserMessage.Response.ResultCode code, String message) {
		logger.info("Response - Failed: " + requestId + "\n" + message + "\n");
	}

	public ChannelFuture connectAsync() {
		return bootstrap.connect(new InetSocketAddress(host, port));
	}

	public void connect() {
		connectAsync().awaitUninterruptibly();
	}

	public void close() {
		clientHandler.close();
	}

	public void run() {

		connect();

	}

	public static void main(String[] args) throws Exception {
		// Print usage if necessary.
		if (args.length != 2) {
			System.err.println("Usage: " + UserClient.class.getSimpleName() + " <host> <port>");
			System.err.println("Example: " + UserClient.class.getSimpleName() + " localhost 8888");
			return;
		}

		// Parse options.
		String host = args[0];
		int port = Integer.parseInt(args[1]);

		new UserClient(host, port).run();
	}

}
