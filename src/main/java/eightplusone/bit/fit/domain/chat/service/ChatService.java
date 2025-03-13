package eightplusone.bit.fit.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.chat.repository.ChatRepository;
import eightplusone.bit.fit.domain.user.repository.UserRedisRepository;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {
	private final ChatRepository chatRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final UserRedisRepository userRedisRepository; // Redis 기반 닉네임 관리
	private final UserRepository userRepository; // 기존 JPA UserRepository

	public ChatService(ChatRepository chatRepository, RedisTemplate<String, Object> redisTemplate,
		UserRedisRepository userRedisRepository, UserRepository userRepository) {
		this.chatRepository = chatRepository;
		this.redisTemplate = redisTemplate;
		this.userRedisRepository = userRedisRepository;
		this.userRepository = userRepository;
	}

	// 메시지 전송 (Redis Pub/Sub 사용)
	public void sendMessage(ChatMessageDto dto, String userId, String sessionId) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		ChatMessage message = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(dto.getCategory())
			.message(dto.getMessage())
			.build();

		chatRepository.saveMessage(message);

		String jsonMessage = objectMapper.writeValueAsString(message);
		String redisKey = "chat-" + sessionId;

		log.info("Redis 발행 메세지 : {}->{}", redisKey, jsonMessage);

		redisTemplate.convertAndSend(redisKey, dto);
	}

	// 특정 채팅방의 최근 메시지 조회
	public List<Object> getRecentMessages(String sessionId) {
		return chatRepository.getRecentMessages(sessionId);
	}

	// 특정 채팅방 데이터 삭제 (강연 종료 후)
	public void clearChat(String sessionId) {
		chatRepository.clearChat(sessionId);
	}

	// 채팅에서 userId를 기반으로 최신 사용자 이름을 가져올 수 있도록 Redis에 name 저장
	public void saveUserName(String userId, String name) {
		userRedisRepository.saveUserName(userId, name);
	}

}
