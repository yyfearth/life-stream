package lifestream.user.queue;

import lifestream.user.data.UserMessage;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class InboundQueue implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(InboundQueue.class.getSimpleName());

	private static final int WORKER_DELAY = 100; // ms
	private static final int MAX_WORKER_COUNT = 255, WORKER_SLEEP_CIRCLES = 100;

	private final RequestQueue batchAddQueue = new RequestQueue();
	private final RequestQueue batchGetQueue = new RequestQueue();
	private final RequestQueue messageQueue = new RequestQueue();

	private final ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKER_COUNT);

	private final OutboundQueue outbound;

	private final Thread worker;

	public InboundQueue(OutboundQueue outbound) {
		this.outbound = outbound;
		worker = new Thread(this, "InboundQueueWorker");
	}

	public void enqueue(MessageEvent e) {
		enqueue(new ChannelMessage<>(e.getChannel(), (UserMessage.Request) e.getMessage()));
	}

	public void enqueue(ChannelMessage<UserMessage.Request> requestMessage) {
		LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> queue;
		UserMessage.Request req = requestMessage.getMessage();
		logger.info("received request: " + req.toString());
		try {
			switch (req.getRequest()) {
				case PING:
					outbound.enqueue(new ChannelMessage<>(requestMessage.getChannel(), genPong(req.getId())));
					return;
				case ADD_USER:
					queue = batchAddQueue; // for batch add
					break;
				case GET_USER:
					queue = batchGetQueue; // for batch get
					break;
				default:
					queue = messageQueue;
					break;
			}
			queue.put(requestMessage);
			startWorker();
		} catch (InterruptedException ex) {
			logger.error("message can not enqueue for inbound", ex);
		}
	}

	// directly put into message queue without batch
	public void put(ChannelMessage<UserMessage.Request> requestMessage) {
		try {
			messageQueue.put(requestMessage);
		} catch (InterruptedException e) {
			logger.error("message can not put to inbound", e);
		}
	}

	// directly take from message queue without batch
	public ChannelMessage<UserMessage.Request> take() {
		try {
			return messageQueue.take();
		} catch (InterruptedException e) {
			logger.error("message can not take from inbound", e);
			return null;
		}
	}

	private UserMessage.Response genPong(String id) {
		return UserMessage.Response.newBuilder()
				.setResult(UserMessage.Response.ResultCode.OK)
				.setRequest(UserMessage.RequestType.PING)
				.setId(id)
				.setTimestamp(System.currentTimeMillis())
				.build();
	}

	protected void startWorker() {
		if (!worker.isAlive()) {
			worker.start();
		}
	}

	public RequestQueue getMessageQueue() {
		return messageQueue;
	}

	public RequestQueue getBatchAddQueue() {
		return batchAddQueue;
	}

	public RequestQueue getBatchGetQueue() {
		return batchGetQueue;
	}

	@Override
	public void run() {
		logger.info("inbound worker start");
		int sleepCircle = 0;
		while (sleepCircle < WORKER_SLEEP_CIRCLES) {
			try {
				if (messageQueue.isEmpty() && batchAddQueue.isEmpty() && batchGetQueue.isEmpty()) {
					sleepCircle++; // count
					Thread.sleep(WORKER_DELAY);
				} else {
					sleepCircle = 0;
					int count = messageQueue.size();
					while (count-- > 0) {
						executor.execute(new MessageProcessWorker(this, outbound));
					}
					Thread.sleep(WORKER_DELAY);
					if (!batchAddQueue.isEmpty()) {
						executor.execute(new BatchAddWorker(this, outbound));
					}
					if (!batchGetQueue.isEmpty()) {
						executor.execute(new BatchGetWorker(this, outbound));
					}
				}
			} catch (InterruptedException e) {
				logger.warn("inbound worker interrupted");
				break;
			}
		}
		logger.info("inbound worker stopped");

	}

	public static class RequestQueue extends LinkedBlockingDeque<ChannelMessage<UserMessage.Request>> {
		// just a alias for now
	}
}
