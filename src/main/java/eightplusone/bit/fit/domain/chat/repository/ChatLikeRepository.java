package eightplusone.bit.fit.domain.chat.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ChatLikeRepository {
	private final RedisTemplate<String, Object> redisTemplate;

	public ChatLikeRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// 좋아요 추가
	public void likeMessage(String userId, String messageId) {
		String likeKey = "like:" + messageId;

		// 좋아요 추가
		redisTemplate.opsForSet().add(likeKey, userId);
	}

	// 좋아요 취소
	public void unlikeMessage(String userId, String messageId) {
		String likeKey = "like:" + messageId;
		redisTemplate.opsForSet().remove(likeKey, userId);
	}

	// 좋아요 개수 조회
	public int getLikeCount(String messageId) {
		String likeKey = "like:" + messageId;
		return Math.toIntExact(redisTemplate.opsForSet().size(likeKey));
	}

	// 사용자가 해당 메시지에 좋아요를 눌렀는지 확인
	public boolean hasLiked(String userId, String messageId) {
		String likeKey = "like:" + messageId;
		return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeKey, userId));
	}
}

