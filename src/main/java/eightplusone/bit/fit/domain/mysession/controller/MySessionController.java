package eightplusone.bit.fit.domain.mysession.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.mysession.dto.MySessionLikedSessionsResponseDto;
import eightplusone.bit.fit.domain.mysession.dto.MySessionScheduleResponseDto;
import eightplusone.bit.fit.domain.mysession.dto.MySessionSessionIdRequestDto;
import eightplusone.bit.fit.domain.mysession.service.MySessionService;
import eightplusone.bit.fit.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/sessions")
public class MySessionController {

	private final MySessionService mySessionService;

	@Operation(summary = "세션 미리 담기 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "세션 미리 담기 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@PostMapping
	public ResponseEntity<ResponseDto<Object>> registerSession(
		@RequestBody MySessionSessionIdRequestDto mySessionSessionIdRequestDto) {
		mySessionService.registerMySession(SecurityContextHolder.getContext().getAuthentication().getName(),
			mySessionSessionIdRequestDto.getSessionId());
		return ResponseEntity.status(CREATED).body(ResponseDto.success(CREATED, "세션 미리 담기 등록 성공", null));
	}

	@Operation(summary = "미리 담은 세션 스케줄 조회", description = "**성공 응답 데이터:**  미리 담은 세션 리스트")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "미리 담은 세션 스케쥴 조회 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@GetMapping
	public ResponseEntity<ResponseDto<List<MySessionScheduleResponseDto>>> getRegisteredSessions() {
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "미리 담은 세션 스케쥴 조회 성공",
			mySessionService.findRegisteredMySessions(
				SecurityContextHolder.getContext().getAuthentication().getName())));
	}

	@Operation(summary = "세션 좋아요 담기 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "세션 좋아요 담기 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@PostMapping("/like")
	public ResponseEntity<ResponseDto<Object>> likeSession(
		@RequestBody MySessionSessionIdRequestDto mySessionSessionIdRequestDto) {
		mySessionService.likeMySession(SecurityContextHolder.getContext().getAuthentication().getName(),
			mySessionSessionIdRequestDto.getSessionId());
		return ResponseEntity.status(CREATED).body(ResponseDto.success(CREATED, "세션 좋아요 담기 성공", null));
	}

	@Operation(summary = "좋아요한 세션 목록 조회", description = "**성공 응답 데이터:**  좋아요한 세션 목록 리스트")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "좋아요한 세션 목록 조회 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@GetMapping("/like")
	public ResponseEntity<ResponseDto<List<MySessionLikedSessionsResponseDto>>> getLikedSessions() {
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "좋아요한 세션 목록 조회 성공",
			mySessionService.findLikedMySessions(
				SecurityContextHolder.getContext().getAuthentication().getName())));
	}

	@Operation(summary = "세션 미리 담기 취소 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "세션 미리 담기 취소 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@DeleteMapping
	public ResponseEntity<ResponseDto<Object>> unregisterSession(
		@RequestBody MySessionSessionIdRequestDto mySessionSessionIdRequestDto) {
		mySessionService.unregisterMySession(SecurityContextHolder.getContext().getAuthentication().getName(),
			mySessionSessionIdRequestDto.getSessionId());
		return ResponseEntity.status(CREATED).body(ResponseDto.success(CREATED, "세션 미리 담기 취소 성공", null));
	}

	@Operation(summary = "세션 좋아요 담기 취소 요청", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "좋아요한 세션 취소 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
		@ApiResponse(responseCode = "500", description = "서버에 오류가 발생했습니다."),
	})
	@DeleteMapping("/like")
	public ResponseEntity<ResponseDto<Object>> unlikeSession(
		@RequestBody MySessionSessionIdRequestDto mySessionSessionIdRequestDto) {
		mySessionService.unlikeMySession(SecurityContextHolder.getContext().getAuthentication().getName(),
			mySessionSessionIdRequestDto.getSessionId());
		return ResponseEntity.status(CREATED).body(ResponseDto.success(CREATED, "좋아요한 세션 취소 성공", null));
	}
}
