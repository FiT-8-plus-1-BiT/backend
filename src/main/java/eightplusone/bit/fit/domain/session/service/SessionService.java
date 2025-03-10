package eightplusone.bit.fit.domain.session.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.entity.enums.CongestionLevel;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final SessionRepository sessionRepository;
	private final String SESSION_CONGESTION_KEY = "session_congestion";
	private final String SESSION_USER_KEY = "session_user";

	// TODO: User ID 매개변수 부분 -> 토큰으로 수정
	// 체크인 시 레디스에 저장
	public void checkIn(Long userId) {
		redisTemplate.opsForHash().put(SESSION_USER_KEY, String.valueOf(userId), "null");
	}

	// TODO: User ID 매개변수 부분 -> 토큰으로 수정
	// 체크아웃 시 레디스에서 삭제
	public void checkOut(Long userId) {
		redisTemplate.opsForHash().delete(SESSION_USER_KEY, String.valueOf(userId));
	}

	// 혼잡도 전송
	public Map<Long, Map<String, Object>> getUpdatedSessionData() {
		List<Session> sessions = sessionRepository.findAll();
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

		return sessions.stream()
			.collect(Collectors.toMap(
				Session::getSessionId,
				session -> {
					double percent = getCongestionPercent(session.getSessionId());
					String level = getCongestionLevel(percent);

					hashOps.put(SESSION_CONGESTION_KEY, session.getSessionId().toString(), level);

					return Map.of("percent", percent, "level", level);
				}
			));
	}

	// 혼잡도 퍼센트
	private double getCongestionPercent(Long sessionId) {
		Session session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new EntityNotFoundException(sessionId + "에 해당하는 세션을 찾을 수 없습니다."));

		long connectedUsers = redisTemplate.opsForHash()
			.values(SESSION_USER_KEY)
			.stream()
			.filter(value -> sessionId.toString().equals(value.toString()))
			.count();

		Integer standardCnt = session.getStandardCount();

		return ((double)connectedUsers / standardCnt) * 100;
	}

	// 혼잡도 레벨
	private String getCongestionLevel(double percent) {
		return CongestionLevel.fromPercent(percent);
	}

	// 혼잡도 변경 시 -> 스트리밍쪽에서 설정
	public void updateAndBroadcastIfChanged(Long sessionId) {
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

		double percent = getCongestionPercent(sessionId);
		String newLevel = getCongestionLevel(percent);

		String previousLevel = hashOps.get(SESSION_CONGESTION_KEY, sessionId);

		if (!newLevel.equals(previousLevel)) {
			hashOps.put(SESSION_CONGESTION_KEY, sessionId.toString(), newLevel);
			redisTemplate.convertAndSend("/sub/ws-room", Map.of(
				"sessionId", sessionId,
				"percent", percent,
				"level", newLevel
			));
		}
	}
}
