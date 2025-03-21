package eightplusone.bit.fit.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

@ExtendWith(MockitoExtension.class)  // ✅ Mockito 기반 테스트 설정
public class ChatLikeRepositoryTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;  // ✅ Mock RedisTemplate

	@Mock
	private SetOperations<String, Object> setOperations;  // ✅ Mock SetOperations

	@InjectMocks
	private ChatLikeRepository chatLikeRepository;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForSet()).thenReturn(setOperations);  // ✅ RedisTemplate 설정
	}

	@Test
	void shouldAddAndRetrieveLikesSuccessfully() {
		// Given
		String userId = "user1";
		String messageId = "message123";

		// Mocking
		when(setOperations.add("like:" + messageId, userId)).thenReturn(1L);
		when(setOperations.size("like:" + messageId)).thenReturn(1L);

		// When
		chatLikeRepository.likeMessage(userId, messageId);
		int likeCount = chatLikeRepository.getLikeCount(messageId);

		// Then
		assertThat(likeCount).isEqualTo(1);
	}

	@Test
	void shouldPreventDuplicateLikes() {
		// Given
		String userId = "user1";
		String messageId = "message123";

		// Mocking
		when(setOperations.add("like:" + messageId, userId)).thenReturn(1L);
		when(setOperations.size("like:" + messageId)).thenReturn(1L);

		// When
		chatLikeRepository.likeMessage(userId, messageId);
		chatLikeRepository.likeMessage(userId, messageId); // Same user liking again

		int likeCount = chatLikeRepository.getLikeCount(messageId);

		// Then
		assertThat(likeCount).isEqualTo(1); // Duplicate like should not increase count
	}

	@Test
	void shouldRemoveLikeSuccessfully() {
		// Given
		String userId = "user1";
		String messageId = "message123";

		// Mocking
		when(setOperations.add("like:" + messageId, userId)).thenReturn(1L);
		when(setOperations.size("like:" + messageId)).thenReturn(1L);
		when(setOperations.remove("like:" + messageId, userId)).thenReturn(1L);
		when(setOperations.size("like:" + messageId)).thenReturn(0L);

		// When
		chatLikeRepository.likeMessage(userId, messageId);
		chatLikeRepository.unlikeMessage(userId, messageId); // Unlike action

		int likeCount = chatLikeRepository.getLikeCount(messageId);

		// Then
		assertThat(likeCount).isEqualTo(0); // Like count should be 0 after unlike
	}

	@Test
	void shouldCheckIfUserLikedMessage() {
		// Given
		String userId = "user1";
		String messageId = "message123";

		// Mocking
		when(setOperations.isMember("like:" + messageId, userId)).thenReturn(true);
		when(setOperations.isMember("like:" + messageId, "user2")).thenReturn(false);

		// When
		boolean user1Liked = chatLikeRepository.hasLiked(userId, messageId);
		boolean user2Liked = chatLikeRepository.hasLiked("user2", messageId);

		// Then
		assertThat(user1Liked).isTrue();
		assertThat(user2Liked).isFalse(); // Another user has not liked
	}
}
