package eightplusone.bit.fit.global.dto;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ResponseDto<T> {

	private final boolean success;
	private final String statusName;
	private final String message;
	private final T response;

	@Builder
	private ResponseDto(boolean success, String statusName, String message, T response) {
		this.success = success;
		this.statusName = statusName;
		this.message = message;
		this.response = response;
	}

	public static <T> ResponseDto<T> success(HttpStatus status, String message, T data) {
		return ResponseDto.<T>builder()
			.success(true)
			.statusName(status.name())
			.message(message)
			.response(data)
			.build();
	}

	public static <T> ResponseDto<T> fail(HttpStatus status, String message) {
		return ResponseDto.<T>builder()
			.success(false)
			.statusName(status.name())
			.message(message)
			.response(null)
			.build();
	}
}
