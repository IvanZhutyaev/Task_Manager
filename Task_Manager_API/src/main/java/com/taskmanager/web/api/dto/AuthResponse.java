package com.taskmanager.web.api.dto;

public record AuthResponse(String token, UserResponse user) {
}
