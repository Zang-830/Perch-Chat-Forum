package com.campus.community.user.dto;

import com.campus.community.user.entity.User;

public record UserView(Long id, String username, String nickname, String avatarUrl, String bio, String role) {
    public static UserView from(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatarUrl(), user.getBio(), user.getRole());
    }
}

