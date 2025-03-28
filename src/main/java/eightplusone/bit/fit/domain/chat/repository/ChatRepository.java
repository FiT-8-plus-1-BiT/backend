package eightplusone.bit.fit.domain.chat.repository;

import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
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
	private static final int MAX_CHAT_SIZE = 1000;

	private final RedisTemplate<String, Object> redisTemplate;
	private final SessionRepository sessionRepository;

	public ChatRepository(RedisTemplate<String, Object> redisTemplate, SessionRepository sessionRepository) {
		this.redisTemplate = redisTemplate;
		this.sessionRepository = sessionRepository;
	}

	public void createChatSession(String sessionId) {
		redisTemplate.opsForValue().set("chat-" + sessionId, "active");
	}

	// 채팅 메시지 저장
	public void saveMessage(ChatMessage message) {
		String chatKey = CHAT_LIST_KEY + message.getSessionId();

		redisTemplate.opsForList().rightPush(chatKey, message);
		Session session = sessionRepository.findById(message.getSessionId()).orElse(null);
		if (session != null) {
			long ttlInSeconds = session.getLectureDuration() + Duration.ofMinutes(30).getSeconds();

			Boolean expireResult = redisTemplate.expire(chatKey, Duration.ofSeconds(ttlInSeconds));
			log.info("🔍 TTL 적용 결과 (true: 성공, false: 실패): {}", expireResult);

			Long ttl = redisTemplate.getExpire(chatKey);
			log.info("🔍 현재 TTL 값 (초 단위): {}", ttl);
		} else {
			log.warn("🚨 세션 정보를 찾을 수 없어 TTL을 설정하지 못했습니다. sessionId: {}", message.getSessionId());
		}

		// 리스트 크기를 1000개로 유지하도록 `trim()` 적용
		redisTemplate.opsForList().trim(chatKey, -MAX_CHAT_SIZE, -1);
	}

	// 특정 채팅방(sessionId)의 최근 메시지 조회
	public List<Object> getRecentMessages(String sessionId) {
		String chatKey = CHAT_LIST_KEY + sessionId;
		List<Object> messages = redisTemplate.opsForList().range(chatKey, 0, -1);

		// Redis 에서 가져온 데이터 확인 로그 추가
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
