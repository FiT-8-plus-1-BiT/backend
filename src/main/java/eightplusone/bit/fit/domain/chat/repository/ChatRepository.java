package eightplusone.bit.fit.domain.chat.repository;

import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {
	private static final Logger log = LoggerFactory.getLogger(ChatRepository.class);
	private static final String CHAT_LIST_KEY = "chat-";
	private static final String LIKES_KEY = "chat_like";
	private static final int MAX_CHAT_SIZE = 1000;

	private final RedisTemplate<String, Object> redisTemplate;

	public ChatRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// 채팅 메시지 저장
	public void saveMessage(ChatMessage message) {
		String chatKey = CHAT_LIST_KEY + message.getSessionId();

		redisTemplate.opsForList().rightPush(chatKey, message);
		redisTemplate.expire(chatKey, Duration.ofHours(2)); // 2시간 후 만료

		// 특정 채팅방의 메시지를 1000개만 유지
		if (redisTemplate.opsForList().size(chatKey) > MAX_CHAT_SIZE) {
			redisTemplate.opsForList().leftPop(chatKey);
		}
	}

	// 특정 채팅방(sessionId)의 최근 메시지 조회
	public List<Object> getRecentMessages(String sessionId) {
		String chatKey = CHAT_LIST_KEY + sessionId;
		List<Object> messages = redisTemplate.opsForList().range(chatKey, 0, -1);

		// 💡 Redis에서 가져온 데이터 확인 로그 추가
		log.info("🔍 Redis에서 가져온 메시지 ({}): {}", chatKey, messages);

		return messages;
	}

	// 특정 채팅방(sessionId) 데이터 삭제
	public void clearChat(String sessionId) {
		String chatKey = CHAT_LIST_KEY + sessionId;
		redisTemplate.delete(chatKey);
	}

	// 채팅방이 존재하는지 확인
	public boolean existsBySessionId(String sessionId) {
		String chatKey = CHAT_LIST_KEY + sessionId;
		return Boolean.TRUE.equals(redisTemplate.hasKey(chatKey));
	}

}
