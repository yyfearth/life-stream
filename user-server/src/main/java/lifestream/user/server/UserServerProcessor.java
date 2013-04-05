package lifestream.user.server;

import com.google.protobuf.GeneratedMessage;
import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import lifestream.user.data.UserMessage;
import org.hibernate.HibernateException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class UserServerProcessor {

	private static final Logger logger = LoggerFactory.getLogger(UserServerProcessor.class.getSimpleName());

	private static final int MAX_WORKER_COUNT = 255;
	private final ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKER_COUNT);

	// private static final int QUEUE_ALERT_SIZE = 1024;
	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> inbound = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Response>> outbound = new LinkedBlockingDeque<>();

	private static final int WORKER_DELAY = 30, WORKER_RETRY = 1000; // ms
	private ThreadGroup threadGroup = new ThreadGroup(UserServerProcessor.class.getSimpleName() + "-ThreadGroup-" + UUID.randomUUID().toString());
	private InBoundWorker inBoundWorker = new InBoundWorker(threadGroup);
	private OutBoundWorker outBoundWorker = new OutBoundWorker(threadGroup);

	protected final UserDao userDao = new UserDao();

	public boolean started() {
		return inBoundWorker.isAlive();
	}

	public void start() {
		if (!started()) {
			inBoundWorker.start();
			outBoundWorker.start();
		}
	}

	// return true if enqueue successfully, false if reject
	public boolean enqueue(MessageEvent e) {
		logger.info("received metadata:");
		UserMessage.Request req = (UserMessage.Request) e.getMessage();
		logger.info(req.toString());
//		if (inbound.size() < QUEUE_ALERT_SIZE) {
		try {
			inbound.put(new ChannelMessage<UserMessage.Request>(e.getChannel(), req));
			return true;
		} catch (InterruptedException ex) {
			logger.error("message not enqueued for processing", ex);
		}
//		}
		return false;
	}

	class InBoundWorker extends Thread {

		public InBoundWorker(ThreadGroup threadGroup) {
			super(threadGroup, InBoundWorker.class.getSimpleName());
		}

		@Override
		public void run() {
			logger.info("inbound worker start");
			while (true) {
				try {
					if (inbound.isEmpty()) {
						Thread.sleep(WORKER_DELAY);
					} else if (!UserDao.isAvailable()) {
						// Circuit breaker?
						logger.warn("data source not available");
						Thread.sleep(WORKER_RETRY);
					} else {
						executor.execute(new UserDataWorker());
					}
				} catch (InterruptedException e) {
					logger.warn("inbound worker interrupted");
					break;
				}
			}
		}
	}

	class OutBoundWorker extends Thread {

		public OutBoundWorker(ThreadGroup threadGroup) {
			super(threadGroup, OutBoundWorker.class.getSimpleName());
		}

		@Override
		public void run() {
			logger.info("outbound worker start");
			while (true) {
				if (!outbound.isEmpty()) {
					try {
						ChannelMessage<UserMessage.Response> responseMessage = outbound.take();
						Channel channel = responseMessage.getChannel();
						if (channel.isWritable()) {
							channel.write(responseMessage.getMessage());
						} else {
							outbound.putFirst(responseMessage);
						}
					} catch (InterruptedException e) {
						logger.error("message not dequeued for processing", e);
					}
				} else {
					try {
						Thread.sleep(WORKER_DELAY);
					} catch (InterruptedException e) {
						logger.warn("outbound worker interrupted");
						break;
					}
				}
			}
		}
	}

	class UserDataWorker implements Runnable {
		@Override
		public void run() {
			try {
				ChannelMessage<UserMessage.Request> requestMessage = inbound.take();
				Channel channel = requestMessage.getChannel();
//				if (channel.isWritable()) {
				UserMessage.Response resp = process(requestMessage.getMessage());
				outbound.put(new ChannelMessage<>(channel, resp));
//				} else {
//					inbound.putFirst(requestMessage);
//				}
			} catch (InterruptedException e) {
				logger.error("message not dequeued for processing", e);
			}
		}

		public UserMessage.Response process(UserMessage.Request req) {
			UserMessage.RequestType type = req.getRequest();
			UserMessage.Response.Builder builder = UserMessage.Response.newBuilder()
					.setId(req.getId())
					.setRequest(type);
			UserMessage.User userBuf = req.getUser();
			UserEntity user;
			try {
				switch (type) {
					case PING:
						// just a ping
						break;
					case ADD_USER:
						user = userDao.create(new UserEntity(userBuf));
						builder.setUser(user.toProtobuf());
						break;
					case GET_USER:
						user = userDao.get(UUID.fromString(req.getUserId()));
						builder.setUser(user.toProtobuf());
						break;
					case UPDATE_USER:
						user = userDao.update(new UserEntity(userBuf));
						builder.setUser(user.toProtobuf());
						break;
					case REMOVE_USER:
						userDao.delete(UUID.fromString(req.getUserId()));
						break;
				}
				builder.setResult(UserMessage.Response.ResultCode.OK);
			} catch (HibernateException ex) {
				builder.setResult(UserMessage.Response.ResultCode.ERROR);
				builder.setMessage(ex.getMessage());
			}
			return builder.setTimestamp(System.currentTimeMillis()).build();
		}
	}

	class ChannelMessage<T extends GeneratedMessage> {
		T message;
		Channel channel;

		public ChannelMessage(Channel channel, T message) {
			this.message = message;
			this.channel = channel;
		}

		T getMessage() {
			return message;
		}

		void setMessage(T message) {
			this.message = message;
		}

		Channel getChannel() {
			return channel;
		}

		void setChannel(Channel channel) {
			this.channel = channel;
		}
	}
}
