package com.campus.community.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.auth.dto.AuthResponse;
import com.campus.community.auth.dto.LoginRequest;
import com.campus.community.auth.dto.RegisterRequest;
import com.campus.community.common.BizException;
import com.campus.community.security.JwtService;
import com.campus.community.security.UserPrincipal;
import com.campus.community.user.dto.UserView;
import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Long existing = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (existing > 0) {
            throw new BizException("用户名已被使用");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setNickname(request.nickname().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException("用户名或密码不正确");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(403, "账号当前不可用");
        }
        return responseFor(user);
    }

    public UserView findUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return UserView.from(user);
    }

    private AuthResponse responseFor(User user) {
        String token = jwtService.createToken(new UserPrincipal(user.getId(), user.getUsername(), user.getRole()));
        return new AuthResponse(token, UserView.from(user));
    }
}

