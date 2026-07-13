package com.chm.aiagent.auth.service;

import com.chm.aiagent.auth.dto.LoginRequest;
import com.chm.aiagent.auth.dto.LoginResponse;
import com.chm.aiagent.exception.BusinessException;
import com.chm.aiagent.exception.ErrorCode;
import com.chm.aiagent.model.User;
import com.chm.aiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 仅在新数据库无用户时作为初始管理员凭据 */
    private static final String FALLBACK_ADMIN_USERNAME = "admin";
    private static final String FALLBACK_ADMIN_PASSWORD = "admin123";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名和密码不能为空");
        }

        // 优先查数据库
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
            String token = jwtTokenProvider.generateToken(user.getUsername());
            return new LoginResponse(token, user.getUsername());
        }

        // 数据库无用户时的 fallback：允许初始管理员登录
        if (FALLBACK_ADMIN_USERNAME.equals(request.getUsername())
                && FALLBACK_ADMIN_PASSWORD.equals(request.getPassword())) {
            log.warn("使用 fallback 管理员凭据登录，请尽快创建正式管理员账户");
            String token = jwtTokenProvider.generateToken(FALLBACK_ADMIN_USERNAME);
            return new LoginResponse(token, FALLBACK_ADMIN_USERNAME);
        }

        throw new BusinessException(ErrorCode.LOGIN_FAILED);
    }
}
