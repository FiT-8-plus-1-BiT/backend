package eightplusone.bit.fit.domain.session.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;

class SessionServiceTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private SessionRepository sessionRepository;

	@InjectMocks
	private SessionService sessionService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
	}

	@Test
	void checkIn() {
		// Given
		Long userId = 1L;

		// When
		sessionService.checkIn(userId);

		// Then
		verify(hashOperations).put("session_user", userId.toString(), "null");
	}

	@Test
	void checkOut() {
		// Given
		Long userId = 1L;

		// When
		sessionService.checkOut(userId);

		// Then
		verify(hashOperations).delete("session_user", userId.toString());
	}

	@Test
	void getUpdatedSessionData() {
		Session session = new Session();
		setField(session, "sessionId", 123L);
		setField(session, "standardCount", 10);

		when(sessionRepository.findAll()).thenReturn(List.of(session));
		when(sessionRepository.findById(123L)).thenReturn(Optional.of(session));

		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123")); // 2명 접속

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);

		Map<Long, Map<String, Object>> result = sessionService.getUpdatedSessionData();

		assertThat(result).containsKey(123L);
		assertThat(result.get(123L)).containsEntry("percent", 20.0);
	}

	@Test
	void updateAndBroadcastIfChanged() {
		// Given
		Long sessionId = 123L;
		double percent = 100.0;
		String currentLevel = "여유"; // 기존 값
		String newLevel = "혼잡"; // 예상값 (변경됨)
		Session session = new Session();
		setField(session, "sessionId", 123L);
		setField(session, "standardCount", 5);

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get("session_congestion", sessionId)).thenReturn(currentLevel);
		when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123", "123", "123", "123"));

		// When
		sessionService.updateAndBroadcastIfChanged(sessionId);

		// Then
		verify(hashOperations).put("session_congestion", sessionId, newLevel);
		verify(redisTemplate).convertAndSend(eq("/sub/ws-room"), argThat(message -> {
			Map<String, Object> msg = (Map<String, Object>)message;
			return msg.get("sessionId").equals(sessionId) &&
				msg.get("percent").equals(percent) &&
				msg.get("level").equals(newLevel);
		}));
	}
}
