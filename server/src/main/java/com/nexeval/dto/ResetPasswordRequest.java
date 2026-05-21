package com.nexeval.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
