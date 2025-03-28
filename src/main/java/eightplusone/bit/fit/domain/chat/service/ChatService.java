package eightplusone.bit.fit.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eightplusone.bit.fit.domain.chat.dto.ChatMessageDto;
import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import eightplusone.bit.fit.domain.chat.repository.ChatLikeRepository;
import eightplusone.bit.fit.domain.chat.repository.ChatRepository;
import eightplusone.bit.fit.domain.user.repository.UserRedisRepository;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {
	private final ChatRepository chatRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final UserRedisRepository userRedisRepository;
	private final ChatLikeRepository chatLikeRepository;
	private final UserRepository userRepository;

	public ChatService(ChatRepository chatRepository, RedisTemplate<String, Object> redisTemplate,
		UserRedisRepository userRedisRepository, UserRepository userRepository, ChatLikeRepository chatLikeRepository) {
		this.chatRepository = chatRepository;
		this.redisTemplate = redisTemplate;
		this.userRedisRepository = userRedisRepository;
		this.chatLikeRepository = chatLikeRepository;
		this.userRepository = userRepository;
	}

	public void createChatSession(Long sessionId) {
		if (chatRepository.existsBySessionId(String.valueOf(sessionId))) {
			log.info("이미 존재하는 채팅방: {}", sessionId);
			return;
		}

		chatRepository.createChatSession(String.valueOf(sessionId));
		log.info("새 채팅방 생성됨: {}", sessionId);
	}

	public void sendMessageWithEmail(ChatMessageDto dto, String email, Long sessionId) throws JsonProcessingException {
		if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_MESSAGE_FORMAT);
		}

		if (dto.getMessage().length() > 300) {
			throw new CustomException(ErrorCode.MESSAGE_TOO_LONG);
		}

		if (!chatRepository.existsBySessionId(String.valueOf(sessionId))) {
			throw new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND);
		}

		var user = userRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		String userId = user.getId().toString();
		String name = user.getName();

		// Redis에 사용자 이름 캐싱
		userRedisRepository.saveUserName(userId, name);

		ChatMessage message = ChatMessage.builder()
			.sessionId(sessionId)
			.userId(userId)
			.category(dto.getCategory())
			.message(dto.getMessage())
			.build();

		chatRepository.saveMessage(message);

		// 질문 메시지라면 ZSet에 추가
		if (message.getCategory() == ChatCategory.QUESTION) {
			String zsetKey = "questions:session:" + sessionId;
			redisTemplate.opsForZSet().add(zsetKey, message.getMessageId(), 0); // 초기 좋아요 수 0
		}

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonMessage = objectMapper.writeValueAsString(message);
		String redisKey = "chat-pub" + sessionId;

		log.info("Redis 발행 메세지 : {} -> {}", redisKey, jsonMessage);
		redisTemplate.convertAndSend(redisKey, dto);
	}

	// 특정 채팅방의 최근 메시지 조회
	public List<Object> getRecentMessages(String sessionId) {
		if (!chatRepository.existsBySessionId(sessionId)) {
			throw new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND);
		}
		return chatRepository.getRecentMessages(sessionId);
	}

	// 특정 채팅방 데이터 삭제 (강연 종료 후)
	public void clearChat(String sessionId) {
		if (!chatRepository.existsBySessionId(sessionId)) {
			throw new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND);
		}
		chatRepository.clearChat(sessionId);
	}

	// 채팅에서 userId를 기반으로 최신 사용자 이름을 가져올 수 있도록 Redis에 name 저장
	public void saveUserName(String userId, String name) {
		userRedisRepository.saveUserName(userId, name);
	}

	// 좋아요 추가
	public void likeMessage(String userId, Long sessionId, String messageId) {
		String likeKey = "like:" + sessionId + ":" + messageId;

		if (chatLikeRepository.hasLiked(likeKey, userId)) {
			throw new CustomException(ErrorCode.DUPLICATE_LIKE);
		}
		chatLikeRepository.likeMessage(likeKey, userId);

		// 좋아요 개수 조회
		int updatedLikeCount = getLikeCount(sessionId, messageId);

		// 좋아요 개수를 Redis Pub/Sub을 통해 전송
		String redisMessage = "{\"messageId\": \"" + messageId + "\", \"likes\": " + updatedLikeCount + "}";
		redisTemplate.convertAndSend("chat-likes", redisMessage);

		log.info("좋아요 변경사항 Redis Pub/Sub 전송: {}", redisMessage);
	}

	// 좋아요 취소
	public void unlikeMessage(String userId, Long sessionId, String messageId) {
		String likeKey = "like:" + sessionId + ":" + messageId;

		if (!chatLikeRepository.hasLiked(likeKey, userId)) {
			throw new CustomException(ErrorCode.CANNOT_UNLIKE);
		}
		chatLikeRepository.unlikeMessage(likeKey, userId, sessionId, messageId);
	}

	public void likeMessageWithEmail(String email, Long sessionId, String messageId) {
		var user = userRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		String userId = user.getId().toString();
		likeMessage(userId, sessionId, messageId);
	}

	public void unlikeMessageWithEmail(String email, Long sessionId, String messageId) {
		var user = userRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		String userId = user.getId().toString();
		unlikeMessage(userId, sessionId, messageId);
	}

	// 좋아요 개수 조회
	public int getLikeCount(Long sessionId, String messageId) {
		String likeKey = "like:" + sessionId + ":" + messageId;
		return chatLikeRepository.getLikeCount(likeKey);
	}

	// 사용자가 해당 메시지에 좋아요를 눌렀는지 확인
	public boolean hasLiked(String userId, Long sessionId, String messageId) {
		String likeKey = "like:" + sessionId + ":" + messageId;
		return chatLikeRepository.hasLiked(likeKey, userId);
	}

	// 특정 세션의 QUESTION 메시지를 좋아요 기준으로 정렬
	public List<ChatMessageDto> getSortedQuestionMessages(Long sessionId) {
		if (!chatRepository.existsBySessionId(String.valueOf(sessionId))) {
			throw new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND);
		}

		List<Object> rawMessages = chatRepository.getRecentMessages(String.valueOf(sessionId));
		log.info("🔍 Redis에서 가져온 원본 메시지: {}", rawMessages);

		List<ChatMessage> messages = rawMessages.stream()
			.map(obj -> {
				if (obj instanceof ChatMessageDto dto) {
					return new ChatMessage(dto.getMessageId(), sessionId, dto.getUserId(), dto.getCategory(),
						dto.getMessage(), dto.getTimestamp());
				} else if (obj instanceof ChatMessage message) {
					return message;
				}
				log.warn("❌ 변환 실패: {}", obj);
				return null;
			})
			.filter(Objects::nonNull)
			.filter(msg -> msg.getCategory() == ChatCategory.QUESTION)
			.collect(Collectors.toList());

		log.info("💬 세션 [{}]에서 가져온 QUESTION 메시지 개수: {}", sessionId, messages.size());

		List<ChatMessage> topLikedMessages = messages.stream()
			.sorted((m1, m2) -> {
				int likeCount1 = getLikeCount(sessionId, m1.getMessageId());
				int likeCount2 = getLikeCount(sessionId, m2.getMessageId());

				log.info("💡 정렬 중: {} ({}개) vs {} ({}개)", m1.getMessageId(), likeCount1, m2.getMessageId(), likeCount2);

				return Integer.compare(likeCount2, likeCount1);
			})
			.limit(3)
			.collect(Collectors.toList());

		messages.removeAll(topLikedMessages);
		topLikedMessages.addAll(messages);

		return topLikedMessages.stream()
			.map(msg -> {
				log.info("🔍 메시지 ID: {}, UserID: {}", msg.getMessageId(), msg.getUserId());
				String userName = userRedisRepository.getUserName(msg.getUserId());
				int likeCount = getLikeCount(sessionId, msg.getMessageId());

				log.info("🔍 userRedisRepository.getUserName({}) → {}", msg.getUserId(), userName);

				return new ChatMessageDto(
					msg.getMessageId(),
					msg.getCategory(),
					msg.getMessage(),
					userName != null ? userName : "알 수 없음",
					msg.getUserId(),
					msg.getSessionId(),
					msg.getTimestamp(),
					likeCount
				);
			})
			.collect(Collectors.toList());
	}

	public List<ChatMessageDto> getZSetSortedQuestions(Long sessionId, int page, int size) {
		String zsetKey = "questions:session:" + sessionId;
		int start = page * size;
		int end = start + size - 1;

		Set<Object> messageIds = redisTemplate.opsForZSet().reverseRange(zsetKey, start, end);
		if (messageIds == null || messageIds.isEmpty())
			return Collections.emptyList();

		return messageIds.stream()
			.map(messageIdObj -> {
				String messageId = messageIdObj.toString();
				String raw = (String)redisTemplate.opsForValue().get("chat:message:" + messageId);
				if (raw == null)
					return null;

				try {
					ObjectMapper mapper = new ObjectMapper();
					ChatMessage msg = mapper.readValue(raw, ChatMessage.class);
					String userName = userRedisRepository.getUserName(msg.getUserId());
					Double score = redisTemplate.opsForZSet().score(zsetKey, messageId);
					int likeCount = score != null ? score.intValue() : 0;

					return new ChatMessageDto(
						msg.getMessageId(),
						msg.getCategory(),
						msg.getMessage(),
						userName != null ? userName : "알 수 없음",
						msg.getUserId(),
						msg.getSessionId(),
						msg.getTimestamp(),
						likeCount
					);
				} catch (JsonProcessingException e) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}

}
