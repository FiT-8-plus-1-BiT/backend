package eightplusone.bit.fit.domain.session.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.session.dto.SessionListResponseDto;
import eightplusone.bit.fit.domain.session.service.SessionService;
import eightplusone.bit.fit.domain.tag.dto.TagDto;
import eightplusone.bit.fit.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;

	@Operation(summary = "세션 전체 조회 및 필터링", description = "**성공 응답 데이터:**  세션 전체 리스트")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "세션 전체 조회 성공"),
	})
	@GetMapping("/all")
	public ResponseEntity<ResponseDto<Page<SessionListResponseDto>>> all(@PageableDefault(size = 9) Pageable pageable,
		TagDto dto) {
		return ResponseEntity.status(OK)
			.body(ResponseDto.success(OK, "세션 전체 리스트 조회 성공", sessionService.getSessionsList(pageable, dto)));
	}

	@Operation(summary = "라이브 중인 전체 조회", description = "**성공 응답 데이터:**  라이브 중인 세션 리스트")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "라이브 중인 세션 조회 성공"),
	})
	@GetMapping("/live")
	public ResponseEntity<ResponseDto<List<SessionListResponseDto>>> onLiveSessions() {
		return ResponseEntity.status(OK)
			.body(ResponseDto.success(OK, "라이브 중인 세션 리스트 조회 성공", sessionService.getLiveSessions()));
	}
}
