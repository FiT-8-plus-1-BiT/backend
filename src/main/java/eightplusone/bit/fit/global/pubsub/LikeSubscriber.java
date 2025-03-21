package eightplusone.bit.fit.global.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LikeSubscriber implements MessageListener {
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LikeSubscriber(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String receivedMessage = new String(message.getBody(), StandardCharsets.UTF_8);
			log.info("좋아요 업데이트 수신: {}", receivedMessage);

			// 좋아요 정보를 WebSocket으로 전송
			messagingTemplate.convertAndSend("/sub/chat-likes", receivedMessage);
			log.info("좋아요 업데이트 WebSocket 전송 완료");

		} catch (Exception e) {
			log.error("좋아요 메시지 처리 중 오류 발생", e);
		}
	}
}
