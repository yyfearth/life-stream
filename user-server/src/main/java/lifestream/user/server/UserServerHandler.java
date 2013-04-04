package lifestream.user.server;

import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import lifestream.user.data.UserMessage;
import org.hibernate.HibernateException;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class.getSimpleName());

	ExecutorService executor = Executors.newFixedThreadPool(255);

//	private LinkedBlockingDeque<MessageEvent> queue = new LinkedBlockingDeque<>();
//	private ThreadGroup threadGroup = new ThreadGroup(UUID.randomUUID().toString());
//	private Thread worker;

//	public UserServerHandler() {
//		worker = new Thread(threadGroup, 255, this){
//
//		};
//	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		logger.info("received metadata:");
		UserMessage.Request req = (UserMessage.Request) e.getMessage();
		logger.info(req.toString());
		Runnable worker = new Worker(e);
		executor.execute(worker);
//		try {
//			queue.put(e);
//		} catch (InterruptedException ex) {
//			logger.error("message not enqueued for processing", ex);
//		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		Throwable ex = e.getCause();
		if (ex instanceof ClosedChannelException) {
			logger.warn("Unexpected channel close from downstream.");
		} else {
			logger.warn("Unexpected exception from downstream.", ex);
		}
		e.getChannel().close();
	}

	class Worker implements Runnable {
		final UserDao userDao = new UserDao();

		MessageEvent event;

		public Worker(MessageEvent messageEvent) {
			event = messageEvent;
		}

		@Override
		public void run() {
//			try {
//				MessageEvent event = queue.take();
//			} catch (InterruptedException e) {
//				logger.error("message not dequeued for processing", e);
//			}
			UserMessage.Response resp = process((UserMessage.Request) event.getMessage());
			Channel channel = event.getChannel();
			if (channel.isWritable()) {
				channel.write(resp);
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

}
