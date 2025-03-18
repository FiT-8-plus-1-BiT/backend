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
		String email = "test@gmail.com";

		// When
		sessionService.checkIn(email);

		// Then
		verify(hashOperations).put("session_user", email, "null");
	}

	@Test
	void checkOut() {
		// Given
		String email = "test@gmail.com";

		// When
		sessionService.checkOut(email);

		// Then
		verify(hashOperations).delete("session_user", email);
	}

	@Test
	void getUpdatedSessionData() {
		Session session = new Session();
		setField(session, "audioChannel", 123);
		setField(session, "standardCount", 10);

		when(sessionRepository.findAll()).thenReturn(List.of(session));
		when(sessionRepository.findByAudioChannel(123)).thenReturn(Optional.of(session));

		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123")); // 2명 접속

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);

		Map<Integer, Map<String, Object>> result = sessionService.getUpdatedSessionData();

		assertThat(result).containsKey(123);
		assertThat(result.get(123)).containsEntry("percent", 20.0);
	}

	@Test
	void updateAndBroadcastIfChanged() {
		// Given
		Integer audioChannel = 123;
		double percent = 100.0;
		String currentLevel = "여유"; // 기존 값
		String newLevel = "혼잡"; // 예상값 (변경됨)
		Session session = new Session();
		setField(session, "audioChannel", 123);
		setField(session, "standardCount", 5);

		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.get("session_congestion", audioChannel)).thenReturn(currentLevel);
		when(sessionRepository.findByAudioChannel(audioChannel)).thenReturn(Optional.of(session));
		when(hashOperations.values("session_user")).thenReturn(List.of("123", "123", "123", "123", "123"));

		// When
		sessionService.updateAndBroadcastIfChanged(audioChannel);

		// Then
		verify(hashOperations).put("session_congestion", audioChannel.toString(), newLevel);
		verify(redisTemplate).convertAndSend(eq("/sub/session"), argThat(message -> {
			Map<String, Object> msg = (Map<String, Object>)message;
			return msg.get("sessionId").equals(audioChannel) &&
				msg.get("percent").equals(percent) &&
				msg.get("level").equals(newLevel);
		}));
	}
}
