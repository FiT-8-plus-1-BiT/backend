package eightplusone.bit.fit.domain.speaker.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.speaker.dto.SpeakerListResponseDto;
import eightplusone.bit.fit.domain.speaker.service.SpeakerService;
import eightplusone.bit.fit.global.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/speaker")
@RequiredArgsConstructor
public class SpeakerController {

	private final SpeakerService speakerService;

	@Operation(summary = "연사 전체 조회", description = "**성공 응답 데이터:** 연사 전체 리스트")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "연사 정보 전체 조회 성공"),
	})
	@GetMapping
	public ResponseEntity<ResponseDto<List<SpeakerListResponseDto>>> get() {
		return ResponseEntity.status(OK)
			.body(ResponseDto.success(OK, "연사 정보 전체 조회 성공", speakerService.getAll()));
	}
}
