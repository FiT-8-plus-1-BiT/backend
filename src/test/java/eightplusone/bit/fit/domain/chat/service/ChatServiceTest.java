package eightplusone.bit.fit.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import eightplusone.bit.fit.domain.chat.repository.ChatLikeRepository;
import eightplusone.bit.fit.domain.chat.repository.ChatRepository;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRedisRepository;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

	@Mock
	private UserRepository userRepository;

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
	private final LocalDateTime timestamp = LocalDateTime.now();
	private final int likes = 0;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		chatService = new ChatService(
			chatRepository,
			redisTemplate,
			userRedisRepository,
			userRepository,
			chatLikeRepository,
			null // 임시 null, 아래에서 직접 주입
		);

		// ✅ objectMapper 직접 생성 후 주입
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		ReflectionTestUtils.setField(chatService, "objectMapper", mapper);

		chatMessageDto = new ChatMessageDto(
			null,
			ChatCategory.GENERAL,
			"테스트 메시지",
			"테스터",
			"1",
			sessionId,
			LocalDateTime.now(),
			0
		);

		chatMessage = ChatMessage.builder()
			.sessionId(sessionId)
			.userId("testUser")
			.category(ChatCategory.GENERAL)
			.message("Hello world!")
			.build();
	}

	@Test
	void createChatSession_shouldCreateIfNotExists() {
		// given
		Long testSessionId = 99L;
		when(chatRepository.existsBySessionId(String.valueOf(testSessionId))).thenReturn(false);
		doNothing().when(chatRepository).createChatSession(String.valueOf(testSessionId));

		// when
		chatService.createChatSession(testSessionId);

		// then
		verify(chatRepository, times(1)).createChatSession(String.valueOf(testSessionId));
	}

	@Test
	void createChatSession_shouldNotCreateIfAlreadyExists() {
		// given
		Long testSessionId = 100L;
		when(chatRepository.existsBySessionId(String.valueOf(testSessionId))).thenReturn(true);

		// when
		chatService.createChatSession(testSessionId);

		// then
		verify(chatRepository, never()).createChatSession(any());
	}

	// 채팅 메시지를 성공적으로 저장하고 레디스에 발행되는지 확인
	@Test
	void sendMessage_success() throws JsonProcessingException {
		// given
		String email = "test@example.com";
		User mockUser = User.of(email, "테스터", "kakao", Role.USER);
		ReflectionTestUtils.setField(mockUser, "id", 1L); // id 필드는 private이므로 강제로 세팅

		when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(true);
		doNothing().when(chatRepository).saveMessage(any(ChatMessage.class));

		// when
		chatService.sendMessageWithEmail(chatMessageDto, email, sessionId);

		// then
		verify(chatRepository, times(1)).saveMessage(any(ChatMessage.class));
	}

	// 존재하지 않는 세션에 메시지를 보내면 예외가 발생하는지 확인
	@Test
	void sendMessage_fails_whenSessionNotFound() {
		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(false);

		CustomException exception = assertThrows(CustomException.class, () ->
			chatService.sendMessageWithEmail(chatMessageDto, userId, sessionId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_SESSION_NOT_FOUND);
	}

	// 사용자가 특정 메시지에 좋아요를 누르면 정상적으로 저장되는지 확인
	@Test
	void likeMessage_success() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(false);
		doNothing().when(chatLikeRepository).likeMessage(likeKey, userId);
		when(chatLikeRepository.getLikeCount(likeKey)).thenReturn(1);

		// ZSetOperations 모킹
		ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
		when(zSetOps.incrementScore("questions:session:" + sessionId, messageId, 1)).thenReturn(1.0);

		chatService.likeMessage(userId, sessionId, messageId);

		verify(chatLikeRepository, times(1)).likeMessage(likeKey, userId);
	}

	@Test
	void likeMessage_shouldPublishLikeUpdateToRedisPubSub() {
		String likeKey = "like:" + sessionId + ":" + messageId;

		when(chatLikeRepository.hasLiked(likeKey, userId)).thenReturn(false);
		doNothing().when(chatLikeRepository).likeMessage(likeKey, userId);
		when(chatLikeRepository.getLikeCount(likeKey)).thenReturn(5);

		// ZSetOperations 모킹
		ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
		when(zSetOps.incrementScore("questions:session:" + sessionId, messageId, 1)).thenReturn(5.0);

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
		ChatMessageDto dto1 = new ChatMessageDto("msg1", ChatCategory.QUESTION, "Q1", "User1", "user1", sessionId,
			LocalDateTime.now(),
			0);
		ChatMessageDto dto2 = new ChatMessageDto("msg2", ChatCategory.QUESTION, "Q2", "User2", "user2", sessionId,
			LocalDateTime.now(),
			0);
		ChatMessageDto dto3 = new ChatMessageDto("msg3", ChatCategory.QUESTION, "Q3", "User3", "user3", sessionId,
			LocalDateTime.now(),
			0);
		ChatMessageDto dto4 = new ChatMessageDto("msg4", ChatCategory.QUESTION, "Q4", "User4", "user4", sessionId,
			LocalDateTime.now(),
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
	void getZSetSortedQuestions_success() throws Exception {
		Long sessionId = 1L;
		String zsetKey = "questions:session:" + sessionId;

		when(chatRepository.existsBySessionId(String.valueOf(sessionId))).thenReturn(true);

		ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

		Set<Object> orderedSet = new LinkedHashSet<>();
		orderedSet.add("msg2");
		orderedSet.add("msg3");
		orderedSet.add("msg1");

		when(zSetOps.reverseRange(zsetKey, 0, 2)).thenReturn(orderedSet);
		when(zSetOps.score(zsetKey, "msg2")).thenReturn(20.0);
		when(zSetOps.score(zsetKey, "msg3")).thenReturn(15.0);
		when(zSetOps.score(zsetKey, "msg1")).thenReturn(10.0);

		ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		ChatMessage m1 = new ChatMessage("msg1", sessionId, "user1", ChatCategory.QUESTION, "Q1", LocalDateTime.now());
		ChatMessage m2 = new ChatMessage("msg2", sessionId, "user2", ChatCategory.QUESTION, "Q2", LocalDateTime.now());
		ChatMessage m3 = new ChatMessage("msg3", sessionId, "user3", ChatCategory.QUESTION, "Q3", LocalDateTime.now());

		when(valueOps.get("chat-messages:msg1")).thenReturn(mapper.writeValueAsString(m1));
		when(valueOps.get("chat-messages:msg2")).thenReturn(mapper.writeValueAsString(m2));
		when(valueOps.get("chat-messages:msg3")).thenReturn(mapper.writeValueAsString(m3));

		when(userRedisRepository.getUserName("user1")).thenReturn("User1");
		when(userRedisRepository.getUserName("user2")).thenReturn("User2");
		when(userRedisRepository.getUserName("user3")).thenReturn("User3");

		List<ChatMessageDto> result = chatService.getZSetSortedQuestions(sessionId, 0, 3);

		assertThat(result).hasSize(3);
		assertThat(result.get(0).getMessageId()).isEqualTo("msg2");
		assertThat(result.get(1).getMessageId()).isEqualTo("msg3");
		assertThat(result.get(2).getMessageId()).isEqualTo("msg1");
	}

}
