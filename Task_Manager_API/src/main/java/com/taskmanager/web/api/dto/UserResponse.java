package com.taskmanager.web.api.dto;

import com.taskmanager.domain.User;

public record UserResponse(Long id, String email, String name) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
