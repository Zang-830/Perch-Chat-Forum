package com.campus.community.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
        String username,
        @NotBlank(message = "请输入昵称")
        @Size(max = 20, message = "昵称不能超过 20 个字符")
        String nickname,
        @NotBlank(message = "请输入密码")
        @Size(min = 8, max = 40, message = "密码长度需为 8-40 位")
        String password
) {
}

