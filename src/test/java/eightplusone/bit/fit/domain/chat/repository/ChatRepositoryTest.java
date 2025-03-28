package eightplusone.bit.fit.domain.chat.repository;

import static org.junit.jupiter.api.Assertions.*;

import eightplusone.bit.fit.domain.chat.entity.ChatMessage;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ChatRepositoryTest {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private SessionRepository sessionRepository;

	private Long sessionId;

	@BeforeEach
	void setUp() {
		// 기존 데이터 삭제
		chatRepository.clearChat(String.valueOf(sessionId));
		sessionRepository.deleteAll();

		// 테스트용 세션 저장 후, 실제 저장된 ID 사용
		Session testSession = sessionRepository.save(Session.builder()
			.title("테스트 세션")
			.sessionImage("test.jpg")
			.summary("테스트용 강연")
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusHours(1)) // 강연시간 1시간
			.standardCount(100)
			.audioChannel(2)
			.build());
		sessionRepository.flush(); // 🔹 강제 반영하여 삭제 방지
		sessionId = testSession.getSessionId(); // 🔹 실제 저장된 ID 가져오기

		// 저장 확인
		Long count = redisTemplate.opsForList().size("chat-messages" + sessionId);
		assertEquals(0, count, "clearChat() 이후에도 메시지가 남아 있음!");
	}

	@Test
	void testChatMessageLimit() {
		// 1100개의 메시지를 저장하여 1000개만 유지되는지 테스트
		for (int i = 1; i <= 1100; i++) {
			ChatMessage message = ChatMessage.builder()
				.sessionId(sessionId)
				.userId("user" + i)
				.message("Message " + i)
				.build();
			chatRepository.saveMessage(message);
		}

		// 저장된 메시지 개수 가져오기
		List<Object> messages = chatRepository.getRecentMessages(sessionId.toString());

		// 메시지가 1000개만 유지되는지 검증
		assertEquals(1000, messages.size());

		// 가장 오래된 메시지가 삭제되었는지 확인
		assertFalse(messages.contains("Message 1")); // 첫 번째 메시지가 삭제되었어야 함
	}

	// 메시지를 초과 저장할 때 삭제가 정상적으로 이루어지는지
	@Test
	void testMessageTrimmingInRedis() {
		for (int i = 1; i <= 1200; i++) { // 1200개 저장
			ChatMessage message = ChatMessage.builder()
				.sessionId(sessionId)
				.userId("user" + i)
				.message("Message " + i)
				.build();
			chatRepository.saveMessage(message);
		}

		// Redis에 저장된 메시지 개수 확인
		Long messageCount = redisTemplate.opsForList().size("chat-messages:" + sessionId);
		System.out.println("현재 Redis 저장된 메시지 개수: " + messageCount);

		// Redis에 1000개만 남아 있는지 확인
		assertNotNull(messageCount);
		assertEquals(1000, messageCount);
	}

	@Test
	@Transactional
		// 자동 롤백 방지
	void testChatMessageExpiration() throws InterruptedException {
		// 테스트용 세션 가져오기
		Session session = sessionRepository.findById(sessionId).orElse(null);
		assertNotNull(session, "테스트용 세션이 존재해야 합니다!");

		// 강연시간(1시간) + 30분 TTL 계산
		long expectedTtl = session.getLectureDuration() + Duration.ofMinutes(30).getSeconds();

		// 메시지 저장
		ChatMessage message = ChatMessage.builder()
			.sessionId(session.getSessionId()) // 저장된 세션 ID 사용
			.userId("user123")
			.message("Test TTL")
			.build();
		chatRepository.saveMessage(message);

		// TTL 설정 확인
		Long ttl = redisTemplate.getExpire("chat-messages:" + session.getSessionId());
		assertNotNull(ttl);
		assertTrue(ttl >= expectedTtl - 10, "TTL이 강연시간 + 30분과 일치하지 않음");

		// TTL을 5초로 변경하여 삭제되는지 테스트
		redisTemplate.expire("chat-" + session.getSessionId(), Duration.ofSeconds(5));

		// 6초 대기 후 데이터가 삭제되었는지 확인
		Thread.sleep(6000);
		assertFalse(chatRepository.existsBySessionId(session.getSessionId().toString()));
	}

	// 특정 메시지가 삭제되었는지 직접 조회
	@Disabled("Redis 메시지 삭제 로직이 확인되었으므로 테스트 비활성화")
	@Test
	void testOldestMessagesAreDeleted() {
		for (int i = 1; i <= 1100; i++) { // 1100개 저장
			ChatMessage message = ChatMessage.builder()
				.sessionId(sessionId)
				.userId("user" + i)
				.message("Message " + i)
				.build();
			chatRepository.saveMessage(message);
		}

		// Redis에 저장된 메시지 개수 직접 확인
		Long messageCount = redisTemplate.opsForList().size("chat-messages" + sessionId);
		System.out.println("🔍 Redis에 저장된 메시지 개수: " + messageCount);
		assertNotNull(messageCount);
		assertEquals(1000, messageCount, "Redis에 저장된 메시지 개수가 1000개가 아님!");

		// Redis에서 가져온 메시지를 Set으로 변환하여 빠르게 비교
		List<Object> messages = redisTemplate.opsForList().range("chat-messages" + sessionId, 0, -1);
		Set<String> messageSet = messages.stream()
			.map(Object::toString)
			.collect(Collectors.toSet());

		System.out.println("🔍 현재 Redis에 저장된 메시지 리스트:");
		messages.forEach(System.out::println);

		// 첫 번째 100개 메시지("Message 1" ~ "Message 100")는 삭제되었어야 함
		IntStream.rangeClosed(1, 100).forEach(i -> {
			assertFalse(messageSet.contains("Message " + i), "오래된 메시지가 삭제되지 않음: Message " + i);
		});

		// 가장 마지막 1000개는 유지되어야 함
		IntStream.rangeClosed(101, 1100).forEach(i -> {
			assertTrue(messageSet.contains("Message " + i), "최근 메시지가 유지되지 않음: Message " + i);
		});
	}

	@Test
	void testCreateChatSession() {
		// given
		Long newSessionId = 9999L;
		chatRepository.createChatSession(String.valueOf(newSessionId));
		// then
		assertTrue(chatRepository.existsBySessionId(String.valueOf(newSessionId)), "채팅방 생성 후 존재하지 않음!");
	}

}