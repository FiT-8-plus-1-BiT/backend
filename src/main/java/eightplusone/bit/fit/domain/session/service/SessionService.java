package eightplusone.bit.fit.domain.session.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import eightplusone.bit.fit.domain.session.dto.SessionListResponseDto;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.entity.enums.CongestionLevel;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import eightplusone.bit.fit.domain.speaker.dto.SpeakerResponseDto;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.speaker.repository.SpeakerRepository;
import eightplusone.bit.fit.domain.tag.dto.TagResponseDto;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import eightplusone.bit.fit.domain.tag.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final SessionRepository sessionRepository;
	private final String SESSION_CONGESTION_KEY = "session_congestion";
	private final String SESSION_USER_KEY = "session_user";
	private final TagRepository tagRepository;
	private final SpeakerRepository speakerRepository;

	// TODO: User ID 매개변수 부분 -> 토큰으로 수정
	// 체크인 시 레디스에 저장
	public void checkIn(String email) {
		redisTemplate.opsForHash().put(SESSION_USER_KEY, email, "null");
	}

	// TODO: User ID 매개변수 부분 -> 토큰으로 수정
	// 체크아웃 시 레디스에서 삭제
	public void checkOut(String email) {
		redisTemplate.opsForHash().delete(SESSION_USER_KEY, email);
	}

	// 혼잡도 전송
	public Map<Integer, Map<String, Object>> getUpdatedSessionData() {
		List<Session> sessions = sessionRepository.findAll();
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

		return sessions.stream()
			.collect(Collectors.toMap(
				Session::getAudioChannel,
				session -> {
					double percent = getCongestionPercent(session.getAudioChannel());
					String level = getCongestionLevel(percent);

					hashOps.put(SESSION_CONGESTION_KEY, session.getAudioChannel().toString(), level);

					return Map.of("percent", percent, "level", level);
				}
			));
	}

	// 혼잡도 퍼센트
	private double getCongestionPercent(Integer audioChannel) {
		Session session = sessionRepository.findByAudioChannel(audioChannel)
			.orElseThrow(() -> new EntityNotFoundException(audioChannel + "에 해당하는 세션을 찾을 수 없습니다."));

		long connectedUsers = redisTemplate.opsForHash()
			.values(SESSION_USER_KEY)
			.stream()
			.filter(value -> audioChannel.toString().equals(value.toString()))
			.count();

		Integer standardCnt = session.getStandardCount();

		return ((double)connectedUsers / standardCnt) * 100;
	}

	// 혼잡도 레벨
	private String getCongestionLevel(double percent) {
		return CongestionLevel.fromPercent(percent);
	}

	// 혼잡도 변경 시 -> 스트리밍쪽에서 설정
	public void updateAndBroadcastIfChanged(Integer audioChannel) {
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

		double percent = getCongestionPercent(audioChannel);
		String newLevel = getCongestionLevel(percent);

		String previousLevel = hashOps.get(SESSION_CONGESTION_KEY, audioChannel);

		if (!newLevel.equals(previousLevel)) {
			hashOps.put(SESSION_CONGESTION_KEY, audioChannel.toString(), newLevel);
			redisTemplate.convertAndSend("/sub/session", Map.of(
				"sessionId", audioChannel,
				"percent", percent,
				"level", newLevel
			));
		}
	}

	public Page<SessionListResponseDto> getSessionsList(Pageable pageable, TagResponseDto tagDto) {
		Page<Object[]> sessions = tagRepository.tagFilterAndSearch(pageable, tagDto);

		return sessions.map(session -> {
			return SessionListResponseDto.from(
				(Session)session[0],
				SpeakerResponseDto.from((Speaker)session[2]),
				TagResponseDto.from((Tag)session[1])
			);
		});
	}
}
