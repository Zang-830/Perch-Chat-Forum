package com.campus.community.comment.dto;

import com.campus.community.user.dto.UserView;

import java.time.LocalDateTime;

public record CommentView(Long id, String content, LocalDateTime createdAt, UserView author) {
}

