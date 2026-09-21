package com.campus.community.auth;

import com.campus.community.auth.dto.AuthResponse;
import com.campus.community.auth.dto.LoginRequest;
import com.campus.community.auth.dto.RegisterRequest;
import com.campus.community.common.ApiResponse;
import com.campus.community.security.CurrentUser;
import com.campus.community.user.dto.UserView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.success(authService.findUser(CurrentUser.id()));
    }
}

