package eightplusone.bit.fit.global.pubsub;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChatSubscriber implements MessageListener {
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatSubscriber(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			// Redis에서 받은 메시지 출력
			String receivedMessage = new String(message.getBody(), StandardCharsets.UTF_8);
			String channelName = new String(message.getChannel(), StandardCharsets.UTF_8);

			log.info("Redis에서 메시지 수신: 채널={}, 메시지={}", channelName, receivedMessage);

			// ChatMessageDto로 변환하여 WebSocket으로 전송
			ChatMessageDto chatMessageDto = objectMapper.readValue(receivedMessage, ChatMessageDto.class);

			String sessionId = channelName.replace("chat-", "");
			String websocketDestination = "/sub/chat/" + sessionId;

			log.info("WebSocket으로 전송 시도: {} -> {}", websocketDestination, chatMessageDto);

			// 메세지 전송
			messagingTemplate.convertAndSend(websocketDestination, chatMessageDto);

			log.info("WebSocket으로 전송 완료: {}", websocketDestination);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
