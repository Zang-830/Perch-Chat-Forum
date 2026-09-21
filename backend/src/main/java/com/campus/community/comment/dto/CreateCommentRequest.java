package com.campus.community.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "请输入评论内容")
        @Size(max = 1000, message = "评论不能超过 1000 个字符") String content
) {
}

