package eightplusone.bit.fit.domain.user.controller;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import eightplusone.bit.fit.domain.user.dto.UserAccountResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileUpdateRequestDto;
import eightplusone.bit.fit.domain.user.service.UserService;
import eightplusone.bit.fit.global.dto.ResponseDto;
import eightplusone.bit.fit.global.utils.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "회원탈퇴 요청", description = "**성공 응답 데이터:**  브라우저 쿠키 초기화"
		+ ", 소셜 로그인 연결끊기가 함께 진행됩니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "회원 탈퇴 완료"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다. 재 로그인 후 탈퇴를 시도하세요."),
	})
	@DeleteMapping
	public ResponseEntity<ResponseDto<Object>> delete() {
		userService.delete(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.status(CREATED)
			.header(HttpHeaders.SET_COOKIE,
				CookieUtil.createCookie(REFRESH_TOKEN_COOKIE_NAME, null, REFRESH_EXPIRATION_DELETE).toString())
			.body(ResponseDto.success(CREATED, "회원 탈퇴 완료", null));
	}

	@Operation(summary = "회원 계정 정보 조회", description = "**성공 응답 데이터:**  회원 계정 정보")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 계정 정보 조회 성공"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
	})
	@GetMapping("/account")
	public ResponseEntity<ResponseDto<UserAccountResponseDto>> getAccount() {
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "회원 계정 정보 조회 성공",
			userService.getAccountInfo(SecurityContextHolder.getContext().getAuthentication().getName())));
	}

	@Operation(summary = "회원 개인 정보 조회", description = "**성공 응답 데이터:**  회원 개인 정보")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 개인 정보 조회 성공"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
	})
	@GetMapping("/profile")
	public ResponseEntity<ResponseDto<UserProfileResponseDto>> getProfile() {
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "회원 개인 정보 조회 성공",
			userService.getProfileInfo(SecurityContextHolder.getContext().getAuthentication().getName())));
	}

	@Operation(summary = "회원 개인 정보 업데이트", description = "**성공 응답 데이터:**  null, 관심 분야는 3개를 선택해야합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 개인 정보 업데이트 성공"),
		@ApiResponse(responseCode = "400", description = "업데이트 필드 값 오류"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
	})
	@PutMapping("/profile")
	public ResponseEntity<ResponseDto<Object>> updateProfile(
		@Valid @RequestBody UserProfileUpdateRequestDto userProfileUpdateRequestDto) {
		userService.updateProfileInfo(SecurityContextHolder.getContext().getAuthentication().getName(),
			userProfileUpdateRequestDto);
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "회원 개인 정보 업데이트 성공", null));
	}

	@Operation(summary = "회원 프로필 사진 업데이트", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 프로필 사진 업데이트 성공"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "500", description = "서버 오류 입니다."),
	})
	@PutMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDto<Object>> updateProfileImage(
		@Parameter(
			content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE),
			description = "업로드할 프로필 이미지",
			required = true)
		@RequestPart MultipartFile requestImage) {
		userService.updateProfileImage(SecurityContextHolder.getContext().getAuthentication().getName(), requestImage);
		return ResponseEntity.status(OK).body(ResponseDto.success(OK, "회원 프로필 사진 업데이트 성공", null));
	}

	@Operation(summary = "회원 프로필 사진 삭제", description = "**성공 응답 데이터:**  null")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "회원 프로필 사진 삭제 성공"),
		@ApiResponse(responseCode = "401", description = "유효한 토큰이 아닙니다."),
		@ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없습니다."),
		@ApiResponse(responseCode = "500", description = "서버 오류 입니다."),
	})
	@DeleteMapping("/profile/image")
	public ResponseEntity<ResponseDto<Object>> deleteProfileImage() {
		userService.deleteProfileImage(SecurityContextHolder.getContext().getAuthentication().getName());
		return ResponseEntity.status(CREATED).body(ResponseDto.success(CREATED, "회원 프로필 사진 삭제 성공", null));
	}
}
