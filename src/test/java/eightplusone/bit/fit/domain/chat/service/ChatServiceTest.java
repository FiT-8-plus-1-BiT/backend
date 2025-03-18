package eightplusone.bit.fit.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import eightplusone.bit.fit.domain.chat.repository.ChatLikeRepository;
import eightplusone.bit.fit.domain.chat.repository.ChatRepository;
import eightplusone.bit.fit.domain.user.repository.UserRedisRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

	@InjectMocks
	private ChatService chatService;

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private ChatLikeRepository chatLikeRepository;

	@Mock
	private UserRedisRepository userRedisRepository;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	private ChatMessageDto testMessageDto;
	private ChatMessage testMessage;
	private String sessionId;
	private String userId;
	private String messageId;

	@BeforeEach
	void setUp() {
		sessionId = "test-session";
		userId = "test-user";
		messageId = "test-message-id";

		testMessageDto = new ChatMessageDto("msg-001", ChatCategory.GENERAL, "테스트 메시지", "테스터", "1");

		testMessage = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(ChatCategory.GENERAL)
			.message("테스트 메시지")
			.build();

	}

	// sendMessage가 채팅메시지를 저장하는지, saveMessage가 한 번 호출되는지 확인
	@Test
	void sendMessage_shouldSaveChatMessage() throws JsonProcessingException {
		// given
		doNothing().when(chatRepository).saveMessage(any(ChatMessage.class));
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);

		// when
		chatService.sendMessage(testMessageDto, userId, sessionId);

		// then
		verify(chatRepository, times(1)).saveMessage(any(ChatMessage.class));
	}

	// 특정 채팅방의 최근 메시지가 올바르게 반환되는지 확인
	@Test
	void getRecentMessages_shouldReturnMessages() {
		// given
		when(chatRepository.getRecentMessages(sessionId)).thenReturn(List.of(testMessage));
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);

		// when
		List<Object> messages = chatService.getRecentMessages(sessionId);

		// then
		assertThat(messages).isNotEmpty();
		assertThat(((ChatMessage)messages.get(0)).getMessage()).isEqualTo("테스트 메시지");
	}

	// 특정 채팅방의 메시지를 삭제하는지 확인
	@Test
	void clearChat_shouldDeleteMessages() {
		// given
		doNothing().when(chatRepository).clearChat(sessionId);
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);

		// when
		chatService.clearChat(sessionId);

		// then
		verify(chatRepository, times(1)).clearChat(sessionId);
	}

	// 특정 사용자의 이름이 Redis에 저장되는지 확인
	@Test
	void saveUserName_shouldStoreUserNameInRedis() {
		// given
		doNothing().when(userRedisRepository).saveUserName(userId, "테스터");

		// when
		chatService.saveUserName(userId, "테스터");

		// then
		verify(userRedisRepository, times(1)).saveUserName(userId, "테스터");
	}

	// 좋아요 개수에 따라 질문 메시지가 정렬되는지 확인
	@Test
	void getSortedQuestionMessages_shouldSortByLikes() {
		// given
		ChatMessage question1 = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(ChatCategory.QUESTION)
			.message("질문 1")
			.build();

		ChatMessage question2 = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(ChatCategory.QUESTION)
			.message("질문 2")
			.build();

		when(chatRepository.getRecentMessages(sessionId)).thenReturn(List.of(question1, question2));
		when(chatRepository.existsBySessionId(sessionId)).thenReturn(true);
		when(chatLikeRepository.getLikeCount(question1.getMessageId())).thenReturn(2);
		when(chatLikeRepository.getLikeCount(question2.getMessageId())).thenReturn(5);
		when(userRedisRepository.getUserName(anyString())).thenReturn("테스터");

		// when
		List<ChatMessageDto> sortedQuestions = chatService.getSortedQuestionMessages(sessionId);

		// then
		assertThat(sortedQuestions).hasSize(2);
		assertThat(sortedQuestions.get(0).getMessage()).isEqualTo("질문 2");
	}

	// 메시지에 좋아요를 추가하면 좋아요 개수가 증가하는지 확인
	@Test
	void likeMessage_shouldIncreaseLikeCount() {
		// given
		doNothing().when(chatLikeRepository).likeMessage(userId, messageId);
		when(chatLikeRepository.getLikeCount(messageId)).thenReturn(1);

		// when
		chatService.likeMessage(userId, messageId);
		int likeCount = chatService.getLikeCount(messageId);

		// then
		verify(chatLikeRepository, times(1)).likeMessage(userId, messageId);
		assertThat(likeCount).isEqualTo(1);
	}

	// 메시지에 좋아요를 취소하면 좋아요 개수가 정상적으로 감소하는지 확인
	@Test
	void unlikeMessage_shouldDecreaseLikeCount() {
		// given
		when(chatLikeRepository.hasLiked(userId, messageId)).thenReturn(true);
		doNothing().when(chatLikeRepository).unlikeMessage(userId, messageId);
		when(chatLikeRepository.getLikeCount(messageId)).thenReturn(0);

		// when
		chatService.unlikeMessage(userId, messageId);
		int likeCount = chatService.getLikeCount(messageId);

		// then
		verify(chatLikeRepository, times(1)).unlikeMessage(userId, messageId);
		assertThat(likeCount).isEqualTo(0);
	}

	// 특정 메시지의 좋아요 개수가 올바르게 반환되는지 확인
	@Test
	void getLikeCount_shouldReturnCorrectCount() {
		// given
		when(chatLikeRepository.getLikeCount(messageId)).thenReturn(5);

		// when
		int likeCount = chatService.getLikeCount(messageId);

		// then
		assertThat(likeCount).isEqualTo(5);
	}

}
