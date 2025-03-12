package eightplusone.bit.fit.domain.session.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.session.service.SessionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/enter")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;

	// TODO : 로그인 구현 후 수정
	@PostMapping("/checkin")
	public ResponseEntity<String> checkIn() {
		sessionService.checkIn(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.ok("체크인 완료 했습니다.");
	}

	@PostMapping("/checkout")
	public ResponseEntity<String> checkOut() {
		sessionService.checkOut(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.ok("체크아웃 완료 했습니다.");
	}
}
