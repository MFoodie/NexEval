package com.nexeval.config;

import com.nexeval.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.internalServerError().body(new ApiResponse(false, ex.getMessage()));
  }
}
