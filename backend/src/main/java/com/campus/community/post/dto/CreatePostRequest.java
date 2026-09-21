package com.campus.community.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotNull(message = "请选择分类") Long categoryId,
        @NotBlank(message = "请输入标题")
        @Size(min = 4, max = 120, message = "标题长度需为 4-120 个字符") String title,
        @NotBlank(message = "请输入正文")
        @Size(min = 10, max = 10000, message = "正文长度需为 10-10000 个字符") String content,
        @Size(max = 9, message = "帖子最多上传 9 张图片") List<@NotBlank String> imageUrls
) {
}
