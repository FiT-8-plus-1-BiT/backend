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

@ExtendWith(MockitoExtension.class)
public class ChatLikeRepositoryTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private SetOperations<String, Object> setOperations;

	@InjectMocks
	private ChatLikeRepository chatLikeRepository;

	private final Long sessionId = 1L;
	private final String messageId = "message123";
	private final String likeKey = "like:" + sessionId + ":" + messageId;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
	}

	@Test
	void shouldAddAndRetrieveLikesSuccessfully() {
		String userId = "user1";

		when(setOperations.add(likeKey, userId)).thenReturn(1L);
		when(setOperations.size(likeKey)).thenReturn(1L);

		chatLikeRepository.likeMessage(likeKey, userId);
		int likeCount = chatLikeRepository.getLikeCount(likeKey);

		assertThat(likeCount).isEqualTo(1);
	}

	@Test
	void shouldPreventDuplicateLikes() {
		String userId = "user1";

		when(setOperations.add(likeKey, userId)).thenReturn(1L);
		when(setOperations.size(likeKey)).thenReturn(1L);

		chatLikeRepository.likeMessage(likeKey, userId);
		chatLikeRepository.likeMessage(likeKey, userId);

		int likeCount = chatLikeRepository.getLikeCount(likeKey);
		assertThat(likeCount).isEqualTo(1);
	}

	@Test
	void shouldRemoveLikeSuccessfully() {
		String userId = "user1";

		when(setOperations.add(likeKey, userId)).thenReturn(1L);
		when(setOperations.remove(likeKey, userId)).thenReturn(1L);
		when(setOperations.size(likeKey)).thenReturn(0L);

		chatLikeRepository.likeMessage(likeKey, userId);
		chatLikeRepository.unlikeMessage(likeKey, userId);

		int likeCount = chatLikeRepository.getLikeCount(likeKey);
		assertThat(likeCount).isEqualTo(0);
	}

	@Test
	void shouldCheckIfUserLikedMessage() {
		when(setOperations.isMember(likeKey, "user1"))
			.thenReturn(true);
		when(setOperations.isMember(likeKey, "user2"))
			.thenReturn(false);

		boolean user1Liked = chatLikeRepository.hasLiked(likeKey, "user1");
		boolean user2Liked = chatLikeRepository.hasLiked(likeKey, "user2");

		assertThat(user1Liked).isTrue();
		assertThat(user2Liked).isFalse();
	}
}
