package com.nexeval.dto;

public record LoginResponse(
	String id,
	String name,
	boolean sex,
	String type,
	String phone,
	String email,
	String avatarUrl,
	StudentInfo studentInfo,
	TeacherInfo teacherInfo
) {
}
