package com.chm.aiagent.service;

import com.chm.aiagent.dto.CreateUserRequest;
import com.chm.aiagent.dto.PasswordVO;
import com.chm.aiagent.dto.UserVO;
import com.chm.aiagent.exception.BusinessException;
import com.chm.aiagent.exception.ErrorCode;
import com.chm.aiagent.model.User;
import com.chm.aiagent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final byte[] aesKey;

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    public AdminService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.jwt.secret}") String jwtSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // 取 JWT secret 的前 32 字节作为 AES-256 key
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.aesKey = new byte[32];
        System.arraycopy(keyBytes, 0, this.aesKey, 0, Math.min(keyBytes.length, 32));
    }

    /** 校验当前用户是否为 SUPER_ADMIN（Service 层双重校验） */
    private void assertSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch("ROLE_SUPER_ADMIN"::equals)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /** 创建测试用户 */
    public UserVO createUser(CreateUserRequest request) {
        assertSuperAdmin();

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在: " + request.getUsername());
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordView(encrypt(request.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.insert(user);

        log.info("SUPER_ADMIN 创建测试用户: {}", request.getUsername());
        return toVO(user);
    }

    /** 测试用户列表（分页） */
    public Map<String, Object> listUsers(int page, int size) {
        assertSuperAdmin();

        int offset = (page - 1) * size;
        List<UserVO> list = userRepository.findAll(offset, size).stream()
                .map(this::toVO)
                .toList();
        int total = userRepository.count();
        return Map.of("list", list, "total", total, "page", page, "size", size);
    }

    /** 查看用户密码（AES 解密 password_view） */
    public PasswordVO viewPassword(String userId) {
        assertSuperAdmin();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + userId));

        if (user.getPasswordView() == null || user.getPasswordView().isBlank()) {
            // 兼容旧用户没有 password_view 的情况
            return new PasswordVO("(该用户密码未存储明文副本，仅支持重置)");
        }

        try {
            return new PasswordVO(decrypt(user.getPasswordView()));
        } catch (Exception e) {
            log.error("解密用户 {} 密码失败", userId, e);
            return new PasswordVO("(解密失败)");
        }
    }

    /** 重置用户密码 */
    public void resetPassword(String userId, String newPassword) {
        assertSuperAdmin();

        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        if (newPassword.length() < 4) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度至少4个字符");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + userId));

        String encoded = passwordEncoder.encode(newPassword);
        String encrypted = encrypt(newPassword);
        userRepository.updatePassword(userId, encoded, encrypted);

        log.info("SUPER_ADMIN 重置用户 {} 的密码", user.getUsername());
    }

    // ========== AES 加解密 ==========

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext，Base64 编码存储
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    private String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[16];
            byte[] ciphertext = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, ciphertext, 0, ciphertext.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    // ========== 辅助方法 ==========

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRole(u.getRole());
        vo.setEnabled(u.isEnabled());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setUpdatedAt(u.getUpdatedAt());
        return vo;
    }
}
