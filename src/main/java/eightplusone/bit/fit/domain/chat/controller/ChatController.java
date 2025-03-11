package eightplusone.bit.fit.domain.chat.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.service.ChatService;
import eightplusone.bit.fit.global.websocket.WebSocketEventListener;

@RestController
@RequestMapping("/chat")
public class ChatController {
	private final ChatService chatService;
	private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	// Redis로 발행
	@MessageMapping("/sendMessage")
	public void sendMessage(@Payload ChatMessageDto message, @RequestHeader("User-Id") String userId) {
		log.info("✅ WebSocket 메시지 수신: {}", message);
		chatService.sendMessage(message, userId);
	}

	// 최근 채팅 메시지 조회 (REST API)
	@GetMapping("/recent")
	public List<Object> getRecentMessages() {
		return chatService.getRecentMessages();
	}

	// 강연 종료 후 데이터 삭제
	@DeleteMapping("/clear")
	public void clearChat() {
		chatService.clearChat();
	}
}
