package eightplusone.bit.fit.domain.session.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/enter")
@RequiredArgsConstructor
public class EnterController {

	private final SessionService sessionService;

	@Operation(summary = "체크인 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "체크인 완료 했습니다."),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
	})
	@PostMapping("/checkin")
	public ResponseEntity<String> checkIn() {
		sessionService.checkIn(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.ok("체크인 완료 했습니다.");
	}

	@Operation(summary = "체크아웃 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "체크아웃 완료 했습니다."),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
	})
	@PostMapping("/checkout")
	public ResponseEntity<String> checkOut() {
		sessionService.checkOut(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.ok("체크아웃 완료 했습니다.");
	}
}
