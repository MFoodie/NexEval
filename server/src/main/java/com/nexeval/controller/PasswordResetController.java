package com.nexeval.controller;

import com.nexeval.dto.ApiResponse;
import com.nexeval.dto.ForgotPasswordRequest;
import com.nexeval.dto.ResetPasswordRequest;
import com.nexeval.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  public PasswordResetController(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/forgot")
  public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    passwordResetService.sendResetCode(request.identifier());
    return ResponseEntity.ok(new ApiResponse(true, "如果该账号存在，重置邮件已发送至对应邮箱，请查收"));
  }

  @PostMapping("/reset")
  public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.ok(new ApiResponse(true, "密码重置成功"));
  }
}
