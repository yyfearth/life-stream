package lifestream.user.queue;

import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MessageProcessWorker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(MessageProcessWorker.class.getSimpleName());
	private ChannelMessage<UserMessage.Request> requestMessage = null;

	private final OutboundQueue outbound;
	private final UserDao userDao = UserDao.getInstance();

	public MessageProcessWorker(InboundQueue inbound, OutboundQueue outbound) {
		this.outbound = outbound;
		// consume
		requestMessage = inbound.take();
	}

	@Override
	public void run() {
		if (requestMessage == null) {
			return;
		}
		Channel channel = requestMessage.getChannel();
		UserMessage.Response resp = process(requestMessage.getMessage());
		outbound.enqueue(new ChannelMessage<>(channel, resp));
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
