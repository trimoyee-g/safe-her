package com.safeher.ratingservice.exception;

import com.safeher.ratingservice.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RatingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(RatingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(DuplicateRatingException.class)
    public ResponseEntity<ApiResponse<Void>> duplicate(DuplicateRatingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> access(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String,String>>> validation(MethodArgumentNotValidException ex) {
        Map<String,String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid", (a,b)->a));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Map<String,String>>builder().success(false).message("Validation failed").data(errors).build());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> general(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Unexpected error"));
    }
}
