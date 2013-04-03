package server;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.ConnectException;

public class NodeChannelHandler extends SimpleChannelHandler {

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
//		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: " + e.getChannel().getRemoteAddress());
//		System.out.println("Channel connected.");
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
//		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: "  + e.getChannel().getRemoteAddress());
//		System.out.println("Channel disconnected.");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: " + e.getChannel().getRemoteAddress());
//		ImageMessage.ServerMessage serverMessage = (ImageMessage.ServerMessage) e.getMessage();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Throwable cause = e.getCause();

		if (cause instanceof ConnectException) {
			return;
		}

		System.out.println("This: " + e.getChannel().getLocalAddress() + " Remote: " + e.getChannel().getRemoteAddress());
		System.out.println("Unepected exception occured in client handler.");

		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}

class DistributedNodePipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();

		// Decoders
		channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		channelPipeline.addLast("protobufDecoder", new ProtobufDecoder(ProtobufMessages.ServerMessage.getDefaultInstance()));

		// Encoder
		channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		channelPipeline.addLast("protobufEncoder", new ProtobufEncoder());

		channelPipeline.addLast("ServerHandler", new NodeChannelHandler());

		return channelPipeline;
	}
}
