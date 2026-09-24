package com.deepfine.inventorysystem.support.exception;

import com.deepfine.inventorysystem.domain.exception.BusinessException;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MALFORMED_REQUEST_MESSAGE = "요청 형식이 올바르지 않습니다.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("비즈니스 예외(서버 측): {}", errorCode, e);
        } else {
            log.info("비즈니스 예외: {} - {}", errorCode, e.getMessage());
        }
        return respond(errorCode, e.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        ServletRequestBindingException.class,
        MissingServletRequestPartException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        log.info("요청 검증 실패: {}", e.getMessage());
        return respond(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMediaTypeNotSupportedException.class,
        MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception e) {
        log.info("요청 형식 오류: {}", e.getMessage());
        return respond(ErrorCode.INVALID_REQUEST, MALFORMED_REQUEST_MESSAGE);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoResource(Exception e) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 406 <br>
     * - 클라이언트가 JSON을 받지 않겠다고 해서 본문 없이 상태만 돌려준다 <br>
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return respond(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode, message));
    }
}
