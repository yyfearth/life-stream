package lifestream.user.server;

import lifestream.user.bean.UserEntity;
import lifestream.user.dao.UserDao;
import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class UserServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class.getSimpleName());

	ExecutorService executor = Executors.newFixedThreadPool(255);

	private LinkedBlockingDeque<MessageEvent> queue = new LinkedBlockingDeque<>();
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
		try {
			queue.put(e);
		} catch (InterruptedException ex) {
			logger.error("message not enqueued for processing", ex);
		}
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
		UserDao userDao = new UserDao();

		@Override
		public void run() {
			try {
				MessageEvent event = queue.take();
				process((UserMessage.Request) event.getMessage());

				Channel channel = event.getChannel();
				if (channel.isWritable()) {
					channel.write(null);
				}
			} catch (InterruptedException e) {
				logger.error("message not dequeued for processing", e);
			}
		}

		public UserMessage.Response process(UserMessage.Request req) {
			UserMessage.RequestType type = req.getRequest();
			UserMessage.Response.Builder builder = UserMessage.Response.newBuilder()
					.setId(req.getId())
					.setRequest(type);
			switch (type) {
				case HELLO:
					// do nothing
					break;
				case ADD_USER:
					userDao.create(new UserEntity(req.getUser()));
					break;
				case GET_USER:
					userDao.get(UUID.fromString(req.getUserId()));
					break;
				case UPDATE_USER:
					// TODO: update user
					break;
				case REMOVE_USER:
					// TODO: delete user
					break;
			}
			return builder.setTimestamp(System.currentTimeMillis()).build();
		}
	}

}
