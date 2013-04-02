package server;

import org.jboss.netty.channel.*;

import java.net.ConnectException;

public class MonitorHandler extends SimpleChannelHandler {

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: "  + e.getChannel().getRemoteAddress());
		System.out.println("Channel connected.");
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: "  + e.getChannel().getRemoteAddress());
		System.out.println("Channel disconnected.");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: "  + e.getChannel().getRemoteAddress());
		System.out.println("Message received");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: "  + e.getChannel().getRemoteAddress());
		Throwable cause = e.getCause();

		if (cause instanceof ConnectException) {
			return;
		}

		System.out.println("Unepected exception occured in client handler.");

		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}