package com.campus.community.auth.dto;

import com.campus.community.user.dto.UserView;

public record AuthResponse(String token, UserView user) {
}

