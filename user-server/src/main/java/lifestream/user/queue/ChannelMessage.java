package lifestream.user.queue;

import com.google.protobuf.GeneratedMessage;
import org.jboss.netty.channel.Channel;

public class ChannelMessage<T extends GeneratedMessage> {
	private T message;
	private Channel channel;

	public ChannelMessage(Channel channel, T message) {
		this.message = message;
		this.channel = channel;
	}

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}
