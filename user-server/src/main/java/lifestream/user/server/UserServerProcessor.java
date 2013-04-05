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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class UserServerProcessor {

	private static final Logger logger = LoggerFactory.getLogger(UserServerProcessor.class.getSimpleName());

	private static final int MAX_WORKER_COUNT = 255;
	private final ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKER_COUNT);

	// private static final int QUEUE_ALERT_SIZE = 1024;
	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> inboundAdd = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> inboundGet = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> inbound = new LinkedBlockingDeque<>();

	private final LinkedBlockingDeque<ChannelMessage<UserMessage.Response>> outbound = new LinkedBlockingDeque<>();

	private static final int WORKER_DELAY = 100, WORKER_RETRY = 1000; // ms
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
		LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> queue;
		try {
			switch (req.getRequest()) {
				case PING:
					pong(e.getChannel(), req); // faster
					return true;
				case ADD_USER:
					queue = inboundAdd; // for batch add
					break;
				case GET_USER:
					queue = inboundGet; // for batch get
					break;
				default:
					queue = inbound;
					break;
			}
			queue.put(new ChannelMessage<>(e.getChannel(), req));
			return true;
		} catch (InterruptedException ex) {
			logger.error("message not enqueued for processing", ex);
		}
		return false;
	}

	// deal with ping
	public void pong(Channel channel, UserMessage.Request req) throws InterruptedException {
		UserMessage.Response resp = UserMessage.Response.newBuilder()
				.setId(req.getId())
				.setRequest(UserMessage.RequestType.PING)
				.setTimestamp(System.currentTimeMillis())
				.setResult(UserMessage.Response.ResultCode.OK)
				.build();
		outbound.put(new ChannelMessage<>(channel, resp));
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
					if (UserDao.isAvailable()) {
						if (!inboundAdd.isEmpty()) {
							executor.execute(new BatchAddUserWorker());
						}
						if (!inboundGet.isEmpty()) {
							executor.execute(new BatchGetUserWorker());
						}
						int count = inbound.size();
						while (count-- > 0) {
							executor.execute(new UserDataWorker());
						}
						Thread.sleep(WORKER_DELAY);
					} else {
						// Circuit breaker?
						logger.warn("data source not available");
						Thread.sleep(WORKER_RETRY);
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

	class BatchAddUserWorker implements Runnable {
		private final HashMap<String, ChannelMessage<UserMessage.Request>> userMap;

		public BatchAddUserWorker() {
			synchronized (inboundAdd) {
				if (inboundAdd.isEmpty()) {
					userMap = null;
				} else if (inboundAdd.size() == 1) {
					userMap = null;
					try {
						inbound.put(inboundAdd.take());
					} catch (InterruptedException e) {
						logger.warn("Failed to move the only req from batch add queue to inbound queue");
					}
				} else {
					userMap = new HashMap<>(inboundAdd.size());
					for (ChannelMessage<UserMessage.Request> req : inboundAdd) {
						UserMessage.User user = req.getMessage().getUser();
						userMap.put(user.getId(), req);
					}
					inboundAdd.clear();
					logger.info("batch add worker inited");
				}
			}

		}

		@Override
		public void run() {
			if (userMap == null) {
				return;
			}
			logger.info("batch add worker run");
			List<UserEntity> userList = new ArrayList<>(userMap.size());
			for (ChannelMessage<UserMessage.Request> req : userMap.values()) {
				userList.add(new UserEntity(req.getMessage().getUser()));
			}
			int step = UserDao.BATCH_SIZE / 2; // for safe
			int max = userList.size();
			for (int i = 0; i < max; i += step) {
				List<UserEntity> users = userList.subList(i, Math.min(i + step, max));
				try {
					userDao.create(users);
					logger.info("Batch added users: " + users.size());
				} catch (HibernateException ex) {
					// fallback
					logger.error("Batch add users failed, use fallback", ex);
					for (UserEntity user : users) {
						try {
							inbound.put(userMap.get(user.getId().toString()));
						} catch (InterruptedException e) {
							logger.error("Cannot enqueue to inbound for batch add fallback", e);
						}
					}
					continue;
				}
				UserMessage.Response.Builder builder = UserMessage.Response.newBuilder()
						.setRequest(UserMessage.RequestType.ADD_USER)
						.setResult(UserMessage.Response.ResultCode.OK)
						.setTimestamp(System.currentTimeMillis());
				for (UserEntity user : users) {
					ChannelMessage<UserMessage.Request> req = userMap.get(user.getId().toString());
					UserMessage.Response resp = builder
							.setId(req.getMessage().getId())
							.setUser(user.toProtobuf())
							.build();
					try {
						outbound.put(new ChannelMessage<>(req.getChannel(), resp));
					} catch (InterruptedException e) {
						logger.error("Cannot enqueue to outbound for batch add", e);
					}
				}
			}
		}
	}

	class BatchGetUserWorker implements Runnable {
		private final HashMap<String, List<ChannelMessage<UserMessage.Request>>> userMap;

		public BatchGetUserWorker() {
			synchronized (inboundGet) {
				if (inboundGet.isEmpty()) {
					userMap = null;
				} else if (inboundGet.size() == 1) {
					userMap = null;
					try {
						inbound.put(inboundGet.take());
					} catch (InterruptedException e) {
						logger.warn("Failed to move the only req from batch get queue to inbound queue");
					}
				} else {
					userMap = new HashMap<>();
					List<ChannelMessage<UserMessage.Request>> reqs;
					for (ChannelMessage<UserMessage.Request> req : inboundGet) {
						String userId = req.getMessage().getUserId();
						if (userMap.containsKey(userId)) {
							reqs = userMap.get(userId);
							reqs.add(req);
						} else {
							reqs = new ArrayList<>();
							reqs.add(req);
							userMap.put(userId, reqs);
						}
					}
					inboundGet.clear();
					logger.info("batch get worker inited");
				}
			}
		}

		@Override
		public void run() {
			if (userMap == null) {
				return;
			}
			logger.info("batch get worker run");
			List<UUID> idList = new ArrayList<>(userMap.size());
			for (String id : userMap.keySet()) {
				idList.add(UUID.fromString(id));
			}
			List<UserEntity> userList = null;
			try {
				if (idList.size() > 1) {
					userList = userDao.get(idList);
					logger.info("Batch got users: " + userList.size());
				}
			} catch (HibernateException ex) {
				logger.error("Batch get users failed", ex);
			}
			String errorMessage = "User not found";
			if (userList == null) {
				logger.info("use fallback get");
				userList = new ArrayList<>(idList.size());
				for (UUID userId : idList) {
					try {
						UserEntity userEntity = userDao.get(userId);
						if (userEntity != null) {
							userList.add(userEntity);
						} else {
							logger.warn("User not found" + userId);
						}
					} catch (HibernateException e) {
						logger.error("Batch get user failed " + userId, e);
						errorMessage = "Get user failed due to db error";
					}
				}
			}
			UserMessage.Response.Builder builder;
			for (UserEntity user : userList) {
				String userId = user.getId().toString();
				builder = UserMessage.Response.newBuilder()
						.setRequest(UserMessage.RequestType.GET_USER)
						.setUser(user.toProtobuf())
						.setResult(UserMessage.Response.ResultCode.OK)
						.setTimestamp(System.currentTimeMillis());
				for (ChannelMessage<UserMessage.Request> req : userMap.get(userId)) {
					try {
						outbound.put(new ChannelMessage<>(req.getChannel(), builder.setId(req.getMessage().getId()).build()));
					} catch (InterruptedException e) {
						logger.error("Cannot enqueue to outbound for batch add", e);
					}
				}
				userMap.remove(userId);
			}
			if (userMap.size() > 0) {
				logger.warn("Some of users are not found by given ids: " + userMap.keySet());
				builder = UserMessage.Response.newBuilder()
						.setRequest(UserMessage.RequestType.GET_USER)
						.setResult(UserMessage.Response.ResultCode.ERROR)
						.setMessage(errorMessage)
						.setTimestamp(System.currentTimeMillis());
				for (List<ChannelMessage<UserMessage.Request>> reqs : userMap.values()) {
					for (ChannelMessage<UserMessage.Request> req : reqs) {
						try {
							outbound.put(new ChannelMessage<>(req.getChannel(), builder.setId(req.getMessage().getId()).build()));
						} catch (InterruptedException e) {
							logger.error("Cannot enqueue to outbound for batch add", e);
						}
					}
				}
			}
		}
	}

	class UserDataWorker implements Runnable {
		private ChannelMessage<UserMessage.Request> requestMessage = null;

		public UserDataWorker() {
			try {
				requestMessage = inbound.take();
			} catch (InterruptedException e) {
				logger.error("message not dequeued for processing", e);
			}
		}

		@Override
		public void run() {
			if (requestMessage == null) {
				return;
			}
			try {
//				ChannelMessage<UserMessage.Request> requestMessage = inbound.take();
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
						if (user == null) {
							throw new Exception("User not found");
						}
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
			} catch (Exception ex) {
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
