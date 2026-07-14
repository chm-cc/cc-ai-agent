package com.chm.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 64, message = "密码长度为4-64个字符")
    private String password;
}
