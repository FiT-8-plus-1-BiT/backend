package eightplusone.bit.fit.global.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

	/**
	 * 400 BAD_REQUEST : 잘못된 요청
	 */
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

	/**
	 * 401 UNAUTHORIZED : 인증 되지 않은 사용자
	 */
	INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "권한 정보가 없는 토큰입니다."),
	EXPIRED_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),

	/**
	 * 403 FORBIDDEN : 권한 없음
	 */
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 자원에 대한 접근 권한이 없습니다."),

	/**
	 * 404 NOT_FOUND : Resource 를 찾을 수 없음
	 */
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 정보의 사용자를 찾을 수 없습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

	/**
	 * 409 : CONFLICT : Resource 의 현재 상태와 충돌
	 */
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일 입니다."),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "데이터가 이미 존재합니다."),
	RESOURCE_CONFLICT(HttpStatus.CONFLICT, "리소스 상태와 충돌이 발생했습니다."),
	OVER_VALIDATION(HttpStatus.CONFLICT,"더 이상 데이터를 저장 할 수 없습니다."),

	/**
	 * 500 INTERNAL_SERVER_ERROR : 서버 오류
	 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다. 관리자에게 문의하세요."),
	DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
