/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package lifestream.user.server;

import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.*;

import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = Logger.getLogger(UserServerHandler.class.getName());

//    private volatile Channel channel = null;

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
//            if (((ChannelStateEvent) e).getState().equals(ChannelState.CONNECTED)) {
//                channel = e.getChannel();
//            }
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		logger.info("received metadata:");
		UserMessage.Request req = (UserMessage.Request) e.getMessage();
		logger.info(req.toString());

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		Throwable ex = e.getCause();
		if (ex instanceof ClosedChannelException) {
			logger.log(Level.WARNING, "Unexpected channel close from downstream.");
		} else {
			logger.log(Level.WARNING, "Unexpected exception from downstream.", ex);
		}
		e.getChannel().close();
//		channel = null;
	}

}
