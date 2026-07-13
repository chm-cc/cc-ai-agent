package com.chm.aiagent.auth.config;

import com.chm.aiagent.model.User;
import com.chm.aiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 应用启动时检查是否存在用户，若无则创建默认管理员。
 * 仅在全新部署时执行一次，已有用户时跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("管理员用户已存在，跳过初始化");
            return;
        }

        User admin = new User();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.insert(admin);
        log.info("默认管理员用户已创建: admin / admin123（请登录后尽快修改密码）");
    }
}
