package eightplusone.bit.fit.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import eightplusone.bit.fit.domain.chat.repository.ChatLikeRepository;
import eightplusone.bit.fit.domain.chat.repository.ChatRepository;
import eightplusone.bit.fit.domain.user.repository.UserRedisRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private UserRedisRepository userRedisRepository;

	@Mock
	private ChatLikeRepository chatLikeRepository;

	@InjectMocks
	private ChatService chatService;

	private ChatMessageDto chatMessageDto;
	private ChatMessage chatMessage;
	private final String sessionId = "testSession";
	private final String userId = "testUser";
	private final String messageId = "testMessage";
	private final String timestamp = "testTimestamp";

	@BeforeEach
	void setUp() {
		chatMessageDto = new ChatMessageDto(
			messageId,
			ChatCategory.GENERAL,
			"Test message",
			"Test User",
			userId,
			sessionId,
			timestamp
		);

		chatMessage = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(ChatCategory.GENERAL)
			.message("Test message")
			.build();
	}

	// 채팅 메시지를 성공적으로 저장하고 레디스에 발행되는지 확인
	@Test
	void sendMessage_success() throws JsonProcessingException {
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);
		doNothing().when(chatRepository).saveMessage(any(ChatMessage.class));

		chatService.sendMessage(chatMessageDto, userId, sessionId);

		verify(chatRepository, times(1)).saveMessage(any(ChatMessage.class));
		verify(redisTemplate, times(1)).convertAndSend(eq("chat-" + sessionId), any(ChatMessageDto.class));
	}

	// 존재하지 않는 세션에 메시지를 보내면 예외가 발생하는지 확인
	@Test
	void sendMessage_fails_whenSessionNotFound() {
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.sendMessage(chatMessageDto, userId, sessionId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND);
	}

	// 사용자가 특정 메시지에 좋아요를 누르면 정상적으로 저장되는지 확인
	@Test
	void likeMessage_success() {
		when(chatLikeRepository.hasLiked(userId, messageId)).thenReturn(false);
		doNothing().when(chatLikeRepository).likeMessage(userId, messageId);

		chatService.likeMessage(userId, messageId);

		verify(chatLikeRepository, times(1)).likeMessage(userId, messageId);
	}

	// 이미 좋아요를 누른 상태에서 다시 누르면 예외가 발생하는지 확인
	@Test
	void likeMessage_fails_whenAlreadyLiked() {
		when(chatLikeRepository.hasLiked(userId, messageId)).thenReturn(true);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.likeMessage(userId, messageId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LIKE);
	}

	// 사용자가 좋아요를 취소할 수 있는지 확인
	@Test
	void unlikeMessage_success() {
		when(chatLikeRepository.hasLiked(userId, messageId)).thenReturn(true);
		doNothing().when(chatLikeRepository).unlikeMessage(userId, messageId);

		chatService.unlikeMessage(userId, messageId);

		verify(chatLikeRepository, times(1)).unlikeMessage(userId, messageId);
	}

	// 좋아요를 누르지 않은 상태에서 취소하려고 하면 예외가 발생하는지 확인
	@Test
	void unlikeMessage_fails_whenNotLiked() {
		when(chatLikeRepository.hasLiked(userId, messageId)).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.unlikeMessage(userId, messageId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_UNLIKE);
	}

	// 특정 채팅방의 최근 메시지를 정상적으로 조회할 수 있는지 확인
	@Test
	void getRecentMessages_success() {
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);
		when(chatRepository.getRecentMessages(sessionId)).thenReturn(List.of(chatMessage));

		List<Object> messages = chatService.getRecentMessages(sessionId);

		assertThat(messages).isNotEmpty();
		verify(chatRepository, times(1)).getRecentMessages(sessionId);
	}

	// 존재하지 않는 세션의 메시지를 조회할 때 예외가 발생하는지 확인
	@Test
	void getRecentMessages_fails_whenSessionNotFound() {
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.getRecentMessages(sessionId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND);
	}

	// 특정 세션에서 QUESTION 카테고리 메시지를 가져와 좋아요 개수 기준으로 정렬되는지 확인
	@Test
	void getSortedQuestionMessages_success() {
		// Given
		when(chatRepository.existsBySessionId("session1")).thenReturn(true);
		when(chatRepository.getRecentMessages("session1")).thenReturn(Arrays.asList(
			new ChatMessageDto("msg1", ChatCategory.QUESTION, "Question 1", "Alice", "user1", "1", "timestamp1"),
			new ChatMessageDto("msg2", ChatCategory.QUESTION, "Question 2", "Bob", "user2", "1", "timestamp2"),
			new ChatMessageDto("msg3", ChatCategory.GENERAL, "General message", "Charlie", "user3", "1", "timestamp3")
		));

		when(chatLikeRepository.getLikeCount("like:msg1")).thenReturn(10);
		when(chatLikeRepository.getLikeCount("like:msg2")).thenReturn(5);
		when(userRedisRepository.getUserName("user1")).thenReturn("Alice");
		when(userRedisRepository.getUserName("user2")).thenReturn("Bob");

		// When
		List<ChatMessageDto> sortedMessages = chatService.getSortedQuestionMessages("session1");

		// Then
		assertThat(sortedMessages).hasSize(2);
		assertThat(sortedMessages.get(0).getMessage()).isEqualTo("Question 1"); // 좋아요 10개
		assertThat(sortedMessages.get(1).getMessage()).isEqualTo("Question 2"); // 좋아요 5개
	}

	// 존재하지 않는 세션에 대한 조회 시 예외가 발생하는지 확인
	@Test
	void getSortedQuestionMessages_fails_whenSessionNotFound() {
		// Given
		when(chatRepository.existsBySessionId("session1")).thenReturn(false);

		// When & Then
		assertThatThrownBy(() -> chatService.getSortedQuestionMessages("session1"))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.CHAT_SESSION_NOT_FOUND.getMessage());
	}
}
