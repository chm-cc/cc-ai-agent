package com.chm.aiagent.auth.service;

import com.chm.aiagent.auth.dto.LoginRequest;
import com.chm.aiagent.auth.dto.LoginResponse;
import com.chm.aiagent.exception.BusinessException;
import com.chm.aiagent.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (!ADMIN_USERNAME.equals(request.getUsername())
                || !ADMIN_PASSWORD.equals(request.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        String token = jwtTokenProvider.generateToken(ADMIN_USERNAME);
        return new LoginResponse(token, ADMIN_USERNAME);
    }
}
