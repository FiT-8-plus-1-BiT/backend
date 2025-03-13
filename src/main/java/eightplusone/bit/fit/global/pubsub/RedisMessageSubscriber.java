package eightplusone.bit.fit.global.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageSubscriber implements MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

	@Override
	public void onMessage(Message message, byte[] pattern) {
		logger.info("Received: {}", new String(message.getBody()));
	}
}
