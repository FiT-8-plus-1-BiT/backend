package eightplusone.bit.fit.global.exception;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import eightplusone.bit.fit.global.dto.ResponseDto;

@RestControllerAdvice
public class CustomExceptionHandler {

	@ExceptionHandler(CustomException.class)
	protected ResponseEntity<ResponseDto<Object>> handleCustomException(CustomException e) {
		ErrorCode errorCode = e.getErrorCode();
		HttpStatus status = errorCode.getHttpStatus();
		return ResponseEntity.status(status).body(ResponseDto.fail(status, errorCode.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ResponseDto<Object>> handleValidationException(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult()
			.getFieldErrors()
			.get(e.getBindingResult().getFieldErrors().size() - 1);
		String message = fieldError.getField() + " 필드의 입력값[ " + fieldError.getRejectedValue() + " ]이 유효하지 않습니다.";
		return ResponseEntity.status(BAD_REQUEST).body(ResponseDto.fail(HttpStatus.BAD_REQUEST, message));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ResponseDto<Object>> handleMaxSizeException(MaxUploadSizeExceededException e) {
		return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
			.body(ResponseDto.fail(EXPECTATION_FAILED, "해당 파일 크기가 너무 큽니다."));
	}
}
