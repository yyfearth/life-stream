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
import org.joda.time.DateTime;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

public class UserClient {
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
				if (response.getResult() == UserMessage.Response.ResultCode.OK) {
					receivedUser(UUID.fromString(response.getId()), response.getUser());
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
		user.setModifiedDateTime(DateTime.now());
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
		UserEntity userEntity = new UserEntity(UUID.fromString(user.getId()));
		userEntity.setEmail(user.getEmail());
		userEntity.setUsername(user.getUsername());
		userEntity.setPassword(user.getPassword());
		userEntity.setCreatedDateTime(new DateTime(user.getCreatedTimestamp()));
		userEntity.setModifiedDateTime(new DateTime(user.getModifiedTimestamp()));
		receivedUser(requestId, userEntity);
	}

	// should be override
	public void receivedUser(UUID requestId, UserEntity user) {

	}

	// should be override
	public void receivedError(UUID requestId, UserMessage.Response.ResultCode code, String message) {

	}

	public void run() { // sync

		// Make a new connection.
		ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(host, port));

		// Wait until the connection is made successfully.
		Channel channel = connectFuture.awaitUninterruptibly().getChannel();

		// Get the handler instance to initiate the request.
		UserClientHandler handler = channel.getPipeline().get(UserClientHandler.class);

		// Request and get the response.
		// TODO: sent request and get response

		// Close the connection.
		channel.close().awaitUninterruptibly();

	}

	public static void main(String[] args) throws Exception {
		// Print usage if necessary.
		if (args.length < 3) {
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
