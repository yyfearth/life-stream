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

public class BatchAddWorker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(BatchAddWorker.class.getSimpleName());

	private final HashMap<String, ChannelMessage<UserMessage.Request>> userMap;
	private final InboundQueue inbound;
	private final OutboundQueue outbound;
	private final UserDao userDao = UserDao.getInstance();

	public BatchAddWorker(InboundQueue inbound, OutboundQueue outbound) {
		this.inbound = inbound;
		this.outbound = outbound;
		// consume
		synchronized (inbound.getBatchAddQueue()) {
			InboundQueue.RequestQueue batchAddQueue = inbound.getBatchAddQueue();
			if (batchAddQueue.isEmpty()) {
				userMap = null;
			} else if (batchAddQueue.size() == 1) {
				userMap = null;
				try {
					inbound.put(batchAddQueue.take());
				} catch (InterruptedException e) {
					logger.warn("Failed to move the only req from batch add queue to inbound queue");
				}
			} else {
				userMap = new HashMap<>(batchAddQueue.size());
				for (ChannelMessage<UserMessage.Request> req : batchAddQueue) {
					UserMessage.User user = req.getMessage().getUser();
					userMap.put(user.getId(), req);
				}
				batchAddQueue.clear();
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
					inbound.put(userMap.get(user.getId().toString()));
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
				outbound.enqueue(new ChannelMessage<>(req.getChannel(), resp));
			}
		}
	}
}
