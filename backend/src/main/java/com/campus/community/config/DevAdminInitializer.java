package com.campus.community.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@Profile("dev")
public class DevAdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;

    public DevAdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder,
                               @Value("${app.dev-admin.enabled:false}") boolean enabled,
                               @Value("${app.dev-admin.username:admin}") String username,
                               @Value("${app.dev-admin.password:}") String password) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "DEV_ADMIN_PASSWORD must be set when DEV_ADMIN_ENABLED is true");
        }
        User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername(username);
        admin.setNickname("社区管理员");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userMapper.insert(admin);
        log.warn("已创建仅供开发环境使用的管理员账号：{}。部署前请关闭 DEV_ADMIN_ENABLED 或修改密码。", username);
    }
}
