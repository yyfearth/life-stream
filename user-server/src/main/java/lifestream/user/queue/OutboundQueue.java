package lifestream.user.queue;

import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class OutboundQueue implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(OutboundQueue.class.getSimpleName());

	private final ResponseQueue outbound = new ResponseQueue();

	private static final int WORKER_DELAY = 100; // ms
	private static final int WORKER_SLEEP_CIRCLES = 100;
	private final Thread worker;

	public OutboundQueue() {
		worker = new Thread(this, "InboundQueueWorker");
	}

	public void enqueue(ChannelMessage<UserMessage.Response> responseMessage) {
		try {
			outbound.put(responseMessage);
			startWorker();
		} catch (InterruptedException ex) {
			logger.error("message can not enqueue for outbound", ex);
		}
	}

	protected void startWorker() {
		if (!worker.isAlive()) {
			try {
				worker.start();
			} catch (IllegalThreadStateException e) {
				logger.warn("start a thread with illegal state: " + worker.getState());
			}
		}
	}

	@Override
	public void run() {
		logger.info("outbound worker start");
		int sleepCircle = 0;
		while (sleepCircle < WORKER_SLEEP_CIRCLES) {
			if (outbound.isEmpty()) {
				sleepCircle++;
			} else {
				sleepCircle = 0;
				while (!outbound.isEmpty()) {
					try {
						ChannelMessage<UserMessage.Response> responseMessage = outbound.take();
						Channel channel = responseMessage.getChannel();
						if (channel.isWritable()) {
							channel.write(responseMessage.getMessage());
						} else if (channel.isConnected()) {
							outbound.put(responseMessage);
						} else {
							logger.warn("discard message since channel has closed: " + responseMessage.getMessage());
						}
					} catch (InterruptedException e) {
						logger.error("message not dequeued for processing", e);
					}
				}
			}
			try {
				Thread.sleep(WORKER_DELAY);
			} catch (InterruptedException e) {
				logger.warn("outbound worker interrupted");
				break;
			}
		}
		logger.info("outbound worker stopped");
	}

	public class ResponseQueue extends LinkedBlockingQueue<ChannelMessage<UserMessage.Response>> {
		// just a alias for now
	}

}



