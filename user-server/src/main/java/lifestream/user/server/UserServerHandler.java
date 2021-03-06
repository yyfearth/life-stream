package lifestream.user.server;

import lifestream.user.queue.InboundQueue;
import lifestream.user.queue.OutboundQueue;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;

public class UserServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class.getSimpleName());

	private final OutboundQueue outbound = new OutboundQueue();
	private final InboundQueue inbound = new InboundQueue(outbound);

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		inbound.enqueue(e);
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

}
