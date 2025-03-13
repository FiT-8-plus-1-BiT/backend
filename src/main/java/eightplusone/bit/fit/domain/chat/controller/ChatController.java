package eightplusone.bit.fit.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.service.ChatService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {
	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	// Redis 로 발행
	@MessageMapping("/chat/{sessionId}")
	public void sendMessage(@DestinationVariable("sessionId") String sessionId,
		@Payload ChatMessageDto message,
		@Header("User-Id") String userId) throws JsonProcessingException {
		chatService.sendMessage(message, userId, sessionId);
	}

	// 특정 채팅방의 최근 메시지 조회
	@GetMapping("/{sessionId}/messages")
	public ResponseEntity<List<Object>> getRecentMessages(@PathVariable String sessionId) {
		List<Object> messages = chatService.getRecentMessages(sessionId);
		return ResponseEntity.ok(messages);
	}

	// 특정 채팅방 데이터 삭제 (강연 종료 후)
	@DeleteMapping("/{sessionId}/clear")
	public ResponseEntity<String> clearChat(@PathVariable String sessionId) {
		chatService.clearChat(sessionId);
		return ResponseEntity.ok("Chat history cleared for session: " + sessionId);
	}

}
