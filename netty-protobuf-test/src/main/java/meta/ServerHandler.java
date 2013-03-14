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
package meta;

import org.jboss.netty.channel.*;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());

    private volatile Channel channel = null;

    public void request() {
        if (channel == null) {
            logger.warning("client not connected");
        } else {
            Meta.Image.Builder builder = Meta.Image.newBuilder();
            builder.setUuid(UUID.randomUUID().toString())
                    .setFilename("test1.jpg")
                    .setProcessed(false);
            channel.write(builder.build());
            logger.info("sent request");
        }
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            logger.info(e.toString());
            if (((ChannelStateEvent) e).getState().equals(ChannelState.CONNECTED)) {
                channel = e.getChannel();
                request(); // test only
            }
        }
        super.handleUpstream(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        logger.info("received metadata:");
        Meta.Image image = (Meta.Image) e.getMessage();
        logger.info(image.toString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
        channel = null;
    }

}
