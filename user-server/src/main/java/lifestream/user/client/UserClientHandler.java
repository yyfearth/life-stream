package lifestream.user.client;

import lifestream.user.bean.UserEntity;
import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.*;
import org.joda.time.DateTime;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = Logger.getLogger(UserClientHandler.class.getName());

	// Stateful properties
	private volatile Channel channel;

	public UUID request(UserMessage.RequestType type, UserEntity user) {
		return request(type, user.toProtobuf());
	}

	public UUID request(UserMessage.RequestType type, UUID userId) {
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(type)
				.setUserId(userId.toString())
				.setTimestamp(DateTime.now().getMillis())
				.build());
	}

	public UUID request(UserMessage.RequestType type, UserMessage.User user) {
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(type)
				.setUserId(user.getId())
				.setUser(user)
				.setTimestamp(DateTime.now().getMillis())
				.build());
	}

	public UUID request() { // say hello
		return request(UserMessage.Request.newBuilder()
				.setId(UUID.randomUUID().toString())
				.setRequest(UserMessage.RequestType.PING)
				.setTimestamp(DateTime.now().getMillis())
				.build());
	}

	protected UUID request(UserMessage.Request request) {
		if (channel == null) {
			logger.warning("not connected yet");
			return null;
		}
//		else if (request.getId() == null) {
//			logger.warning("a request id is required, auto generated for it");
//			request = request.toBuilder().setId(UUID.randomUUID().toString()).build();
//		}
		channel.write(request);
		logger.info("sent request: " + request);
		return UUID.fromString(request.getId());
	}

	// should be override
	public void received(UserMessage.Response response) {
		logger.info("Received response: " + response);
	}

	public boolean isReady() {
		return channel != null;
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
		received((UserMessage.Response) e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}
}

