package eightplusone.bit.fit.domain.mysession.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eightplusone.bit.fit.domain.mysession.dto.MySessionLikedSessionsResponseDto;
import eightplusone.bit.fit.domain.mysession.dto.MySessionScheduleResponseDto;
import eightplusone.bit.fit.domain.mysession.entity.MySession;
import eightplusone.bit.fit.domain.mysession.enums.MySessionType;
import eightplusone.bit.fit.domain.mysession.repository.MySessionRepository;
import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.session.repository.SessionRepository;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MySessionService {

	private final MySessionRepository mySessionRepository;
	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;

	@Transactional
	public void registerMySession(String email, Long sessionId) {
		User user = userRepository.findLoginUserByEmail(email);
		Session session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		mySessionRepository.save(MySession.register(user, session));
	}

	public List<MySessionScheduleResponseDto> findRegisteredMySessions(String email) {
		User user = userRepository.findLoginUserByEmail(email);

		// 등록세션 조회
		List<Long> mySessionIds = mySessionRepository.findSessionsByUserIdAndType(user.getId(),
				MySessionType.REGISTER)
			.stream()
			.map(mySession -> mySession.getSession().getSessionId())
			.toList();

		// 전체세션조회
		List<Session> allSessions = sessionRepository.findAll();

		return allSessions.stream()
			.map(session -> MySessionScheduleResponseDto.from(session, mySessionIds.contains(session.getSessionId())))
			.collect(Collectors.toList());
	}

	@Transactional
	public void likeMySession(String email, Long sessionId) {
		User user = userRepository.findLoginUserByEmail(email);
		Session session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		mySessionRepository.save(MySession.like(user, session));
	}

	public List<MySessionLikedSessionsResponseDto> findLikedMySessions(String email) {
		User user = userRepository.findLoginUserByEmail(email);
		return mySessionRepository.findSessionsByUserIdAndType(user.getId(), MySessionType.LIKE)
			.stream()
			.map(mySession -> MySessionLikedSessionsResponseDto.from(mySession.getSession()))
			.collect(Collectors.toList());
	}

	@Transactional
	public void unregisterMySession(String email, Long sessionId) {
		User user = userRepository.findLoginUserByEmail(email);
		int deletedCount = mySessionRepository.deleteByUserIdAndSessionIdAndType(user.getId(), sessionId,
			MySessionType.REGISTER);
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}
	}

	@Transactional
	public void unlikeMySession(String email, Long sessionId) {
		User user = userRepository.findLoginUserByEmail(email);
		int deletedCount = mySessionRepository.deleteByUserIdAndSessionIdAndType(user.getId(), sessionId,
			MySessionType.LIKE);
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}
	}
}
