package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.CreateUserRequest;
import com.chm.aiagent.dto.PasswordVO;
import com.chm.aiagent.dto.ResetPasswordRequest;
import com.chm.aiagent.dto.UserVO;
import com.chm.aiagent.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 创建测试用户 */
    @PostMapping("/users")
    public Result<UserVO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok(adminService.createUser(request));
    }

    /** 测试用户列表（分页） */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminService.listUsers(page, size));
    }

    /** 查看用户密码 */
    @GetMapping("/users/{id}/password")
    public Result<PasswordVO> viewPassword(@PathVariable String id) {
        return Result.ok(adminService.viewPassword(id));
    }

    /** 重置用户密码 */
    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable String id,
                                       @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request.getPassword());
        return Result.ok();
    }
}
