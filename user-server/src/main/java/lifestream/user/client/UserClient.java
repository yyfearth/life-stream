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
import java.util.*;
import java.util.concurrent.Executors;

public class UserClient {
	// private static final Logger logger = LoggerFactory.getLogger(UserClient.class.getSimpleName());

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

		clientHandler = new UserClientHandler();
	}

	@Override
	public void finalize() throws Throwable {
		// Shut down all thread pools to exit.
		bootstrap.releaseExternalResources();

		super.finalize();
	}

	public UUID ping(ResultRequestResponseHandler handler) {
		return clientHandler.request(handler);
	}

	public UUID getUser(UserEntity user, UserRequestResponseHandler handler) {
		return getUser(user.getId(), handler);
	}

	public UUID getUser(UUID userId, UserRequestResponseHandler handler) {
		return requestUser(UserMessage.RequestType.GET_USER, userId, handler);
	}

	public UUID addUser(UserEntity user, UserRequestResponseHandler handler) {
		return requestUser(UserMessage.RequestType.ADD_USER, user, handler);
	}

	public UUID updateUser(UserEntity user, UserRequestResponseHandler handler) {
		return requestUser(UserMessage.RequestType.UPDATE_USER, user, handler);
	}

	public UUID removeUser(UserEntity user, ResultRequestResponseHandler handler) {
		return removeUser(user.getId(), handler);
	}

	public UUID removeUser(UUID userId, ResultRequestResponseHandler handler) {
		return requestUser(UserMessage.RequestType.UPDATE_USER, userId, handler);
	}

	public UUID requestUser(UserMessage.RequestType type, UserEntity user, UserClientHandler.RequestResponseHandler handler) {
		return clientHandler.request(type, user, handler);
	}

	public UUID requestUser(UserMessage.RequestType type, UUID userId, UserClientHandler.RequestResponseHandler handler) {
		return clientHandler.request(type, userId, handler);
	}

	public Channel connect() {
		// Start the connection attempt.
		ChannelFuture channel = bootstrap.connect(new InetSocketAddress(host, port));

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess()) {
			return channel.getChannel();
		} else {
			throw new RuntimeException("Not able to establish connection to server");
		}
	}

	public void close() {
		clientHandler.close();
	}

	public static abstract class ResultRequestResponseHandler extends UserClientHandler.RequestResponseHandler {
		@Override
		public void received(UserMessage.Response response) {
			if (response.getResult() == UserMessage.Response.ResultCode.OK) {
				receivedOK(new Date(response.getTimestamp()));
			} else {
				receivedError(response.getResult(), response.getMessage());
			}
		}

		public abstract void receivedOK(Date timestamp);
//		public void receivedOK(Date timestamp) {
//			logger.info("Response - Success: " + requestId + "\n" + timestamp + "\n");
//		}

		public abstract void receivedError(UserMessage.Response.ResultCode code, String message);
//		public void receivedError(UserMessage.Response.ResultCode code, String message) {
//			logger.info("Response - Failed: " + requestId + "\n" + message + "\n");
//		}
	}

	public static abstract class UserRequestResponseHandler extends UserClientHandler.RequestResponseHandler {

		@Override
		public void received(UserMessage.Response response) {
			if (response.getResult() == UserMessage.Response.ResultCode.OK) {
				if (response.getRequest() != UserMessage.RequestType.PING) {
					receivedUser(response.getUser());
				}
			} else {
				receivedError(response.getResult(), response.getMessage());
			}
		}

		private void receivedUser(UserMessage.User user) {
			receivedUser(user == null ? null : new UserEntity(user));
		}

		public abstract void receivedUser(UserEntity user);
//		public void receivedUser(UserEntity user) {
//			logger.info("Response - Success: " + requestId + "\n" + user + "\n");
//		}

		public abstract void receivedError(UserMessage.Response.ResultCode code, String message);
//		public void receivedError(UserMessage.Response.ResultCode code, String message) {
//			logger.info("Response - Failed: " + requestId + "\n" + message + "\n");
//		}
	}

	private UserEntity genUser() { // for test

		String username = "test." + UUID.randomUUID(); // generator.nextInt(65535);
		UserEntity user = new UserEntity();
		user.setEmail(username + "@ab.com");
		user.setUsername(username);
		user.setPassword(new String(new char[64]).replace('\0', '0'));

		return user;
	}

	public void run() {

		connect();

		try {  // for test

			Random generator = new Random();
			// batch add
			int delay, userCount = 1000; // generator.nextInt(100);

			List<UUID> userIds = new ArrayList<>(userCount);
			while (userCount-- > 0) {
				final UserEntity user = genUser();
				final UUID id = user.getId();
				userIds.add(user.getId());
				addUser(user, new UserClient.UserRequestResponseHandler() {

					@Override
					public void receivedUser(UserEntity user) {
						if (id.equals(user.getId())) {
							System.out.println("Add Passed: \n" + user + "\n");
						} else {
							System.out.println("Add Failed: user id not matched\n" + user + "\n");
						}
					}

					@Override
					public void receivedError(UserMessage.Response.ResultCode code, String message) {
						System.out.println("Add Failed: " + message + "\n");
					}
				});
				delay = generator.nextInt(111);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
			Thread.sleep(1000);
			// batch get
			for (final UUID id : userIds) {
				getUser(id, new UserClient.UserRequestResponseHandler() {

					@Override
					public void receivedUser(UserEntity user) {
						if (id.equals(user.getId())) {
							System.out.println("Get Passed: \n" + user + "\n");
						} else {
							System.out.println("Get Failed: user id not matched\n" + user + "\n");
						}
					}

					@Override
					public void receivedError(UserMessage.Response.ResultCode code, String message) {
						System.out.println("Get Failed: " + message + "\n");
					}
				});
				delay = generator.nextInt(111);
				if (delay > 100) {
					Thread.sleep(delay);
				}
			}
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

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
