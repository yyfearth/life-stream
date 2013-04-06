package lifestream.user.client;

import lifestream.user.bean.UserEntity;
import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(UserClientHandler.class.getSimpleName());

	// Stateful properties
	private volatile Channel channel;

	private final Map<String, RequestResponseHandler> handlerMap = new ConcurrentHashMap<>();

	public ChannelFuture request(UserMessage.RequestType type, UserEntity user, RequestResponseHandler handler) {
		return request(type, user.toProtobuf(), handler);
	}

	public ChannelFuture request(UserMessage.RequestType type, UUID userId, RequestResponseHandler handler) {
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(type)
				.setUserId(userId.toString())
				.setTimestamp(System.currentTimeMillis())
				.build(), handler);
	}

	public ChannelFuture request(UserMessage.RequestType type, UserMessage.User user, RequestResponseHandler handler) {
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(type)
				.setUserId(user.getId())
				.setUser(user)
				.setTimestamp(System.currentTimeMillis())
				.build(), handler);
	}

	public ChannelFuture request(RequestResponseHandler handler) { // ping
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(UserMessage.RequestType.PING)
				.setTimestamp(System.currentTimeMillis())
				.build(), handler);
	}

	protected ChannelFuture request(UserMessage.Request request, RequestResponseHandler handler) {
		if (channel == null) {
			logger.warn("Not connected yet");
			return null;
		}
		String id = request.getId();
		if (id != null && (handler == null || handler.beforeSend(request))) {
			// logger.info("Send request: " + request);
			if (handler != null) {
				handlerMap.put(request.getId(), handler);
			}
			if (channel.isWritable()) {
				return channel.write(request);
			} else {
				logger.warn("Request rejected since channel not writable: " + request);
				return null;
			}
		} else {
			logger.info("Request canceled before send: " + request);
			return null;
		}
	}

	// could be override
//	public void received(UserMessage.Response response) {
//		logger.info("Received response: " + response);
//	}

	public boolean isReady() {
		return channel != null;
	}

	public void close() {
		if (channel != null) {
			// channel.getCloseFuture().awaitUninterruptibly();
			channel.close().awaitUninterruptibly();
			channel = null;
		}
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		channel = e.getChannel();
		super.channelOpen(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
		UserMessage.Response resp = (UserMessage.Response) e.getMessage();
		String id = resp.getId();
		// received(resp);
		RequestResponseHandler handler = handlerMap.get(id);
		if (handler != null) {
			handlerMap.remove(id);
			handler.received(resp);
		} else {
			logger.warn("handler not found: " + id);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error("Unexpected exception from downstream.", e.getCause());
		close();
	}

	public abstract static class RequestResponseHandler {
		// can be override
		public boolean beforeSend(UserMessage.Request request) {
			return true;
		}

		public abstract void received(UserMessage.Response response);
	}
}

