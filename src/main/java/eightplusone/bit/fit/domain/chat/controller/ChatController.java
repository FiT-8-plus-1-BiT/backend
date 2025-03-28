package eightplusone.bit.fit.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.service.ChatService;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "채팅 관련 API")
public class ChatController {
	private final ChatService chatService;
	private final UserRepository userRepository;

	public ChatController(ChatService chatService, UserRepository userRepository) {
		this.chatService = chatService;
		this.userRepository = userRepository;
	}

	@Operation(summary = "채팅방 생성", description = "채팅 세션이 없으면 새로 생성하고, 있으면 그대로 사용합니다.")
	@PostMapping("/session/{sessionId}")
	public ResponseEntity<String> createChatSession(
		@Parameter(description = "채팅 세션 ID", example = "1234") @PathVariable Long sessionId
	) {
		chatService.createChatSession(sessionId);
		return ResponseEntity.ok("채팅방 생성 완료 또는 이미 존재: " + sessionId);
	}

	@Operation(summary = "메시지 전송", description = "특정 채팅 세션에 메시지를 전송합니다.")
	@MessageMapping("/chat/{sessionId}")
	public void sendMessage(
		@Parameter(description = "채팅 세션 ID", example = "1234") @DestinationVariable("sessionId") String sessionId,
		@Payload ChatMessageDto message,
		@Parameter(description = "사용자 ID", example = "user123") @Header("simpUser") Principal principal
	) throws JsonProcessingException {
		chatService.sendMessageWithEmail(message, principal.getName(), Long.valueOf(sessionId));
	}

	@Operation(summary = "최근 메시지 조회", description = "특정 채팅방의 최근 메시지를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "성공적으로 메시지를 반환함")
	})
	@GetMapping("/{sessionId}/messages")
	public ResponseEntity<List<Object>> getRecentMessages(
		@Parameter(description = "채팅 세션 ID", example = "1234") @PathVariable String sessionId
	) {
		List<Object> messages = chatService.getRecentMessages(sessionId);
		return ResponseEntity.ok(messages);
	}

	@Operation(summary = "채팅 데이터 삭제", description = "특정 채팅방의 데이터를 삭제합니다. (강연 종료 후)")
	@DeleteMapping("/{sessionId}/clear")
	public ResponseEntity<String> clearChat(
		@Parameter(description = "채팅 세션 ID", example = "1234") @PathVariable String sessionId
	) {
		chatService.clearChat(sessionId);
		return ResponseEntity.ok("Chat history cleared for session: " + sessionId);
	}

	@Operation(summary = "메시지 좋아요", description = "특정 메시지에 좋아요를 추가합니다.")
	@PostMapping("/like/{sessionId}/{messageId}")
	public void likeMessage(
		@Parameter(description = "세션 ID", example = "1234") @PathVariable Long sessionId,
		@Parameter(description = "메시지 ID", example = "msg123") @PathVariable String messageId,
		@Parameter(description = "사용자 ID", example = "user123") Principal principal
	) {
		chatService.likeMessageWithEmail(principal.getName(), sessionId, messageId);
	}

	@Operation(summary = "메시지 좋아요 취소", description = "특정 메시지의 좋아요를 취소합니다.")
	@PostMapping("/unlike/{sessionId}/{messageId}")
	public void unlikeMessage(
		@Parameter(description = "세션 ID", example = "1234") @PathVariable Long sessionId,
		@Parameter(description = "메시지 ID", example = "msg123") @PathVariable String messageId,
		@Parameter(description = "사용자 ID", example = "user123") Principal principal
	) {
		chatService.unlikeMessageWithEmail(principal.getName(), sessionId, messageId);
	}

	@Operation(summary = "질문 메시지 정렬", description = "특정 세션의 질문 메시지에서 가장 좋아요를 많이 받은 3개만 반환합니다.")
	@GetMapping("/questions/{sessionId}")
	public List<ChatMessageDto> getSortedQuestions(
		@Parameter(description = "채팅 세션 ID", example = "1234") @PathVariable String sessionId
	) {
		return chatService.getSortedQuestionMessages(Long.valueOf(sessionId));
	}

	@GetMapping("/likes/{sessionId}/{messageId}")
	public ResponseEntity<Boolean> hasLiked(@PathVariable Long sessionId,
		@PathVariable String messageId,
		Principal principal) {
		String email = principal.getName();
		Long userId = userRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getId();

		boolean result = chatService.hasLiked(userId.toString(), sessionId, messageId);
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "ZSet 기반 좋아요 정렬 질문 페이징", description = "ZSet으로 좋아요 순으로 정렬된 질문을 페이징하여 반환합니다.")
	@GetMapping("/questions/zset/{sessionId}")
	public ResponseEntity<List<ChatMessageDto>> getZSetSortedQuestions(
		@PathVariable Long sessionId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "3") int size
	) {
		List<ChatMessageDto> result = chatService.getZSetSortedQuestions(sessionId, page, size);
		return ResponseEntity.ok(result);
	}

}
