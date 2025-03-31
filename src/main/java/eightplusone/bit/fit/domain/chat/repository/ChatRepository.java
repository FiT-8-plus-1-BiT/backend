package eightplusone.bit.fit.domain.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	private static final String CHAT_MESSAGE_KEY_PREFIX = "chat-messages:";  // 메시지 저장용
	private static final String CHAT_SESSION_KEY_PREFIX = "chat-session:";   // 세션 존재 확인용
	private static final int MAX_CHAT_SIZE = 1000;

	private final RedisTemplate<String, Object> redisTemplate;
	private final SessionRepository sessionRepository;
	private final ObjectMapper objectMapper;

	public ChatRepository(RedisTemplate<String, Object> redisTemplate, SessionRepository sessionRepository,
		ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.sessionRepository = sessionRepository;
		this.objectMapper = objectMapper;
	}

	public void createChatSession(String sessionId) {
		redisTemplate.opsForValue().set(CHAT_SESSION_KEY_PREFIX + sessionId, "active");
	}

	// 채팅 메시지 저장
	public void saveMessage(ChatMessage message) {
		String chatKey = CHAT_MESSAGE_KEY_PREFIX + message.getSessionId();
		String messageKey = "chat-messages:" + message.getMessageId();

		try {
			// 1. 리스트에 저장
			redisTemplate.opsForList().rightPush(chatKey, message);

			// 2. messageId 기준으로 개별 저장
			String jsonMessage = objectMapper.writeValueAsString(message);
			redisTemplate.opsForValue().set(messageKey, jsonMessage);

			// 3. TTL 설정
			Session session = sessionRepository.findById(message.getSessionId()).orElse(null);
			if (session != null) {
				long ttlInSeconds = session.getLectureDuration() + Duration.ofMinutes(30).getSeconds();

				Boolean expireResult = redisTemplate.expire(chatKey, Duration.ofSeconds(ttlInSeconds));
				log.info("🔍 TTL 적용 결과 (true: 성공, false: 실패): {}", expireResult);

				Long ttl = redisTemplate.getExpire(chatKey);
				log.info("🔍 현재 TTL 값 (초 단위): {}", ttl);

				// messageId 기준 저장된 데이터에도 동일 TTL 적용
				redisTemplate.expire(messageKey, Duration.ofSeconds(ttlInSeconds));
			} else {
				log.warn("🚨 세션 정보를 찾을 수 없어 TTL을 설정하지 못했습니다. sessionId: {}", message.getSessionId());
			}

			// 4. 리스트 크기 유지
			redisTemplate.opsForList().trim(chatKey, -MAX_CHAT_SIZE, -1);
		} catch (Exception e) {
			log.error("❌ 메시지 저장 중 오류 발생", e);
		}
	}

	// 특정 채팅방(sessionId)의 최근 메시지 조회
	public List<Object> getRecentMessages(String sessionId) {
		String chatKey = CHAT_MESSAGE_KEY_PREFIX + sessionId;
		List<Object> messages = redisTemplate.opsForList().range(chatKey, 0, -1);

		// Redis 에서 가져온 데이터 확인 로그 추가
		log.info("🔍 Redis에서 가져온 메시지 ({}): {}", chatKey, messages);

		return messages;
	}

	// 특정 채팅방(sessionId) 데이터 삭제
	public void clearChat(String sessionId) {
		String chatKey = CHAT_MESSAGE_KEY_PREFIX + sessionId;
		redisTemplate.delete(chatKey);
	}

	// 채팅방이 존재하는지 확인
	public boolean existsBySessionId(String sessionId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(CHAT_SESSION_KEY_PREFIX + sessionId));
	}

}
