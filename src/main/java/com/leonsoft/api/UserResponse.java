package com.leonsoft.api;

import com.leonsoft.models.User;

public record UserResponse(
        Integer id,
        String username,
        String email,
        String role,
        boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive());
    }
}
