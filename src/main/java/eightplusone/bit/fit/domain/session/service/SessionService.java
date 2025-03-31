package eightplusone.bit.fit.domain.session.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eightplusone.bit.fit.domain.interest.mapper.InterestMapper;
import eightplusone.bit.fit.domain.mysession.repository.MySessionRepository;
import eightplusone.bit.fit.domain.session.dto.ScoredSession;
import eightplusone.bit.fit.domain.session.dto.SessionDetailResponseDto;
import eightplusone.bit.fit.domain.session.dto.SessionListResponseDto;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.entity.enums.CongestionLevel;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import eightplusone.bit.fit.domain.speaker.dto.SpeakerResponseDto;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import eightplusone.bit.fit.domain.tag.entity.enums.CareerLevel;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SessionService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final SessionRepository sessionRepository;
	private final UserRepository userRepository;
	private final MySessionRepository mySessionRepository;
	private final String SESSION_CONGESTION_KEY = "session_congestion";
	private final String SESSION_USER_KEY = "session_user";

	// 체크인 시 레디스에 저장
	public void checkIn(String email) {
		redisTemplate.opsForHash().put(SESSION_USER_KEY, email, "null");
	}

	// 체크아웃 시 레디스에서 삭제
	public void checkOut(String email) {
		redisTemplate.opsForHash().delete(SESSION_USER_KEY, email);
	}

	// 혼잡도 전송
	public Map<Integer, Map<String, Object>> getUpdatedSessionData() {
		List<Session> sessions = sessionRepository.findByIsLiveTrue();
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

		return sessions.stream()
			.collect(Collectors.toMap(
				Session::getAudioChannel,
				session -> {
					double percent = getCongestionPercent(session.getAudioChannel());
					String level = getCongestionLevel(percent);

					log.info("혼잡도 계산 - 채널 {}: {}% ({})", session.getAudioChannel(), percent, level);

					hashOps.put(SESSION_CONGESTION_KEY, session.getAudioChannel().toString(), level);

					return Map.of("percent", percent, "level", level);
				}
			));
	}

	// 혼잡도 퍼센트
	private double getCongestionPercent(Integer audioChannel) {
		Session session = sessionRepository.findByAudioChannel(audioChannel)
			.orElseThrow(() -> {
				log.error("해당 세션 없음: {}", audioChannel);
				return new EntityNotFoundException(audioChannel + "에 해당하는 세션을 찾을 수 없습니다.");
			});

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

		String previousLevel = hashOps.get(SESSION_CONGESTION_KEY, audioChannel.toString());

		if (!newLevel.equals(previousLevel)) {
			hashOps.put(SESSION_CONGESTION_KEY, audioChannel.toString(), newLevel);
			redisTemplate.convertAndSend("/sub/session", Map.of(
				"sessionId", audioChannel,
				"percent", percent,
				"level", newLevel
			));
		}
	}

	private Long getAuthenticatedUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
			|| "anonymousUser".equals(authentication.getPrincipal())) {
			return null;
		}

		String email = authentication.getName();
		User user = userRepository.findLoginUserByEmail(email);
		return user != null ? user.getId() : null;
	}

	public Page<SessionListResponseDto> getSessionsList(Pageable pageable, TagDto tagDto) {
		Long userId = getAuthenticatedUserId();
		Page<Object[]> sessions = sessionRepository.tagFilterAndSearch(pageable, tagDto, userId);

		return sessions.map(session -> {
			return SessionListResponseDto.from(
				(Session)session[0],
				SpeakerResponseDto.from((Speaker)session[2]),
				TagDto.from((Tag)session[1]),
				session[3] != null
			);
		});
	}

	public List<SessionListResponseDto> getLiveSessions() {
		Long userId = getAuthenticatedUserId();

		List<Object[]> sessions = sessionRepository.findLiveSessionsWithSpeakerAndTag(userId);

		return sessions.stream().map(session -> {
			return SessionListResponseDto.from(
				(Session)session[0],
				SpeakerResponseDto.from((Speaker)session[1]),
				TagDto.from((Tag)session[2]),
				session[3] != null
			);
		}).toList();
	}

	public void updateSessionUserAudioChannel(String email, Integer audioChannel) {
		String value = audioChannel == null ? "null" : audioChannel.toString();
		redisTemplate.opsForHash().put(SESSION_USER_KEY, email, value);
	}

	@Transactional
	public void deleteSessionData(Integer audioChannel) {
		redisTemplate.opsForHash().entries(SESSION_USER_KEY)
			.forEach((key, value) -> {
				if (value.toString().equals(audioChannel.toString())) {
					redisTemplate.opsForHash().put(SESSION_USER_KEY, key, "null");
				}
			});
		sessionRepository.updateIsLiveByAudioChannel(audioChannel, false);
	}

	@Transactional
	public void activateSessionLive(Integer audioChannel) {
		sessionRepository.updateIsLiveByAudioChannel(audioChannel, true);
	}

	public SessionDetailResponseDto getSessionDetail(Long sessionId) {
		Long userId = getAuthenticatedUserId();
		Session session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

		Object[] result = sessionRepository.findSessionDetailWithSpeakerAndTag(session.getSessionId(), userId);
		return SessionDetailResponseDto.from(
			(Session)result[0],
			SpeakerResponseDto.from((Speaker)result[1]),
			TagDto.from((Tag)result[2]),
			result[3] != null,
			(Long)result[4]
		);
	}

	private Set<Long> getExcludedSessionIds(Long userId) {
		return mySessionRepository.findByUserId(userId).stream()
			.map(mySession -> mySession.getSession().getSessionId())
			.collect(Collectors.toSet());
	}

	private List<String> getInterestNames(User user) {
		return user.getMyInterests().stream()
			.map(myInterest -> myInterest.getInterest().getName())
			.toList();
	}

	private int calculateScore(Tag tag, List<String> tagFields, String userLevel) {
		int score = 0;
		if (tagFields.contains(tag.getField()))
			score += 3;
		score += CareerLevel.calculateScore(userLevel, tag.getLevel());
		return score;
	}

	public List<SessionListResponseDto> recommendSessionsByInterests() {
		User user = userRepository.findLoginUserByEmail(
			SecurityContextHolder.getContext().getAuthentication().getName());
		Long userId = user.getId();

		Set<Long> excludedSessionIds = getExcludedSessionIds(userId);
		String level = CareerLevel.mapToTagLevel(user.getYears().name());

		List<String> tagFields = InterestMapper.mapToTagFields(getInterestNames(user));
		List<Object[]> allSessions = sessionRepository.findAllWithSpeakerAndTag();

		List<ScoredSession> scoredList = allSessions.stream()
			.filter(data -> !excludedSessionIds.contains(((Session)data[0]).getSessionId()))
			.map(data -> {
				Session session = (Session)data[0];
				Tag tag = (Tag)data[1];
				Speaker speaker = (Speaker)data[2];

				int score = calculateScore(tag, tagFields, level);
				return new ScoredSession(score, session, tag, speaker);
			})
			.sorted()
			.toList();

		return scoredList.stream()
			.map(scored -> SessionListResponseDto.from(
				scored.getSession(),
				SpeakerResponseDto.from(scored.getSpeaker()),
				TagDto.from(scored.getTag()),
				false
			)).toList();
	}
}
