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
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

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
	private final Long sessionId = 1L;
	private final String userId = "testUser";
	private final String messageId = "testMessage";
	private final String timestamp = "testTimestamp";
	private final int likes = 0;

	@BeforeEach
	void setUp() {
		chatMessageDto = new ChatMessageDto(
			messageId,
			ChatCategory.GENERAL,
			"Test message",
			"Test User",
			userId,
			sessionId,
			timestamp,
			likes
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
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(true);
		doNothing().when(chatRepository).saveMessage(any(ChatMessage.class));

		chatService.sendMessage(chatMessageDto, userId, sessionId);

		verify(chatRepository, times(1)).saveMessage(any(ChatMessage.class));
		verify(redisTemplate, times(1)).convertAndSend(eq("chat-" + sessionId), any(ChatMessageDto.class));
	}

	// 존재하지 않는 세션에 메시지를 보내면 예외가 발생하는지 확인
	@Test
	void sendMessage_fails_whenSessionNotFound() {
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.sendMessage(chatMessageDto, userId, sessionId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND);
	}

	// 사용자가 특정 메시지에 좋아요를 누르면 정상적으로 저장되는지 확인
	@Test
	void likeMessage_success() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(false);
		doNothing().when(chatLikeRepository).likeMessage(likeKey, userId);
		when(chatLikeRepository.getLikeCount(likeKey)).thenReturn(1);

		chatService.likeMessage(userId, sessionId, messageId);

		verify(chatLikeRepository, times(1)).likeMessage(likeKey, userId);
	}

	@Test
	void likeMessage_shouldPublishLikeUpdateToRedisPubSub() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(false);
		doNothing().when(chatLikeRepository).likeMessage(likeKey, userId);
		when(chatLikeRepository.getLikeCount(likeKey)).thenReturn(5);

		chatService.likeMessage(userId, sessionId, messageId);

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).convertAndSend(eq("chat-likes"), messageCaptor.capture());

		String publishedMessage = messageCaptor.getValue();
		assertThat(publishedMessage).contains("\"messageId\": \"" + messageId + "\"");
		assertThat(publishedMessage).contains("\"likes\": 5");
	}

	// 이미 좋아요를 누른 상태에서 다시 누르면 예외가 발생하는지 확인
	@Test
	void likeMessage_fails_whenAlreadyLiked() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(true);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.likeMessage(userId, sessionId, messageId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LIKE);
	}

	// 사용자가 좋아요를 취소할 수 있는지 확인
	@Test
	void unlikeMessage_success() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(true);
		doNothing().when(chatLikeRepository).unlikeMessage(likeKey, userId, sessionId, messageId);

		chatService.unlikeMessage(userId, sessionId, messageId);

		verify(chatLikeRepository, times(1)).unlikeMessage(likeKey, userId, sessionId, messageId);
	}

	// 좋아요를 누르지 않은 상태에서 취소하려고 하면 예외가 발생하는지 확인
	@Test
	void unlikeMessage_fails_whenNotLiked() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.unlikeMessage(userId, sessionId, messageId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_UNLIKE);
	}

	// 특정 채팅방의 최근 메시지를 정상적으로 조회할 수 있는지 확인
	@Test
	void getRecentMessages_success() {
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(true);
		when(chatRepository.getRecentMessages(String.valueOf(sessionId))).thenReturn(List.of(chatMessage));

		List<Object> messages = chatService.getRecentMessages(String.valueOf(sessionId));

		assertThat(messages).isNotEmpty();
		verify(chatRepository, times(1)).getRecentMessages(String.valueOf(sessionId));
	}

	// 존재하지 않는 세션의 메시지를 조회할 때 예외가 발생하는지 확인
	@Test
	void getRecentMessages_fails_whenSessionNotFound() {
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.getRecentMessages(String.valueOf(sessionId)));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND);
	}

	// 특정 세션에서 QUESTION 카테고리 메시지를 가져와 좋아요 개수 기준 상위 3개만 가져오는지 확인
	@Test
	void getSortedQuestionMessages_returnsTop3Only() {
		// given
		Long sessionId = 1L;

		// 메시지 4개 (QUESTION)
		ChatMessageDto dto1 = new ChatMessageDto("msg1", ChatCategory.QUESTION, "Q1", "User1", "user1", sessionId, "t1",
			0);
		ChatMessageDto dto2 = new ChatMessageDto("msg2", ChatCategory.QUESTION, "Q2", "User2", "user2", sessionId, "t2",
			0);
		ChatMessageDto dto3 = new ChatMessageDto("msg3", ChatCategory.QUESTION, "Q3", "User3", "user3", sessionId, "t3",
			0);
		ChatMessageDto dto4 = new ChatMessageDto("msg4", ChatCategory.QUESTION, "Q4", "User4", "user4", sessionId, "t4",
			0);

		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(true);
		when(chatRepository.getRecentMessages(String.valueOf(sessionId))).thenReturn(List.of(dto1, dto2, dto3, dto4));

		when(userRedisRepository.getUserName(any())).thenReturn("Tester");

		// 좋아요 개수
		when(chatLikeRepository.getLikeCount("like:1:msg1")).thenReturn(3); // 중간
		when(chatLikeRepository.getLikeCount("like:1:msg2")).thenReturn(5); // 가장 많음
		when(chatLikeRepository.getLikeCount("like:1:msg3")).thenReturn(2); // 적음
		when(chatLikeRepository.getLikeCount("like:1:msg4")).thenReturn(4); // 두 번째

		// when
		List<ChatMessageDto> result = chatService.getSortedQuestionMessages(sessionId);

		// then
		assertThat(result).hasSize(4); // 전체는 4개
		List<ChatMessageDto> top3 = result.subList(0, 3); // 상위 3개

		// 좋아요 순서대로 정렬되었는지 확인
		assertThat(top3.get(0).getMessageId()).isEqualTo("msg2"); // 5
		assertThat(top3.get(1).getMessageId()).isEqualTo("msg4"); // 4
		assertThat(top3.get(2).getMessageId()).isEqualTo("msg1"); // 3
	}

	@SuppressWarnings("unchecked")
	@Test
	void getZSetSortedQuestions_success() {
		// given
		String zsetKey = "questions:session:" + sessionId;

		// ZSetOperations mock
		ZSetOperations<String, Object> zSetOps = Mockito.mock(ZSetOperations.class);
		Mockito.when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
		Mockito.when(zSetOps.reverseRange(zsetKey, 0, 2))
			.thenReturn(new LinkedHashSet<>(List.of("msg3", "msg1", "msg2")));
		Mockito.when(zSetOps.score(zsetKey, "msg3")).thenReturn(10.0);
		Mockito.when(zSetOps.score(zsetKey, "msg1")).thenReturn(5.0);
		Mockito.when(zSetOps.score(zsetKey, "msg2")).thenReturn(2.0);

		// ValueOperations mock + doReturn().when() 방식
		ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
		Mockito.doReturn(valueOps).when(redisTemplate).opsForValue(); // 핵심 차이점
		Mockito.when(valueOps.get("chat:message:msg3"))
			.thenReturn(
				"{\"messageId\":\"msg3\",\"sessionId\":1,\"userId\":\"user3\",\"category\":\"QUESTION\",\"message\":\"Q3\",\"timestamp\":\"t3\"}");
		Mockito.when(valueOps.get("chat:message:msg1"))
			.thenReturn(
				"{\"messageId\":\"msg1\",\"sessionId\":1,\"userId\":\"user1\",\"category\":\"QUESTION\",\"message\":\"Q1\",\"timestamp\":\"t1\"}");
		Mockito.when(valueOps.get("chat:message:msg2"))
			.thenReturn(
				"{\"messageId\":\"msg2\",\"sessionId\":1,\"userId\":\"user2\",\"category\":\"QUESTION\",\"message\":\"Q2\",\"timestamp\":\"t2\"}");

		// 사용자 이름
		when(userRedisRepository.getUserName("user1")).thenReturn("User1");
		when(userRedisRepository.getUserName("user2")).thenReturn("User2");
		when(userRedisRepository.getUserName("user3")).thenReturn("User3");

		// when
		List<ChatMessageDto> result = chatService.getZSetSortedQuestions(sessionId, 0, 3);

		// then
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getMessageId()).isEqualTo("msg3");
		assertThat(result.get(1).getMessageId()).isEqualTo("msg1");
		assertThat(result.get(2).getMessageId()).isEqualTo("msg2");
	}

}
