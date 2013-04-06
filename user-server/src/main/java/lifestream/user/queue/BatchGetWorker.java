package lifestream.user.queue;

import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import lifestream.user.data.UserMessage;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BatchGetWorker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(BatchGetWorker.class.getSimpleName());

	private final HashMap<String, List<ChannelMessage<UserMessage.Request>>> userMap;
	private final OutboundQueue outbound;
	private final UserDao userDao = new UserDao();

	public BatchGetWorker(InboundQueue inbound, OutboundQueue outbound) {
		this.outbound = outbound;
		// consume
		synchronized (inbound.getBatchGetQueue()) {
			InboundQueue.RequestQueue batchGetQueue = inbound.getBatchGetQueue();
			if (batchGetQueue.isEmpty()) {
				userMap = null;
			} else if (batchGetQueue.size() == 1) {
				userMap = null;
				try {
					inbound.put(batchGetQueue.take());
				} catch (InterruptedException e) {
					logger.warn("Failed to move the only req from batch get queue to inbound queue");
				}
			} else {
				userMap = new HashMap<>();
				List<ChannelMessage<UserMessage.Request>> reqs;
				for (ChannelMessage<UserMessage.Request> req : batchGetQueue) {
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
				batchGetQueue.clear();
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
				outbound.enqueue(new ChannelMessage<>(req.getChannel(), builder.setId(req.getMessage().getId()).build()));
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
			for (List<ChannelMessage<UserMessage.Request>> requests : userMap.values()) {
				for (ChannelMessage<UserMessage.Request> req : requests) {
					outbound.enqueue(new ChannelMessage<>(req.getChannel(), builder.setId(req.getMessage().getId()).build()));
				}
			}
		}
	}
}
