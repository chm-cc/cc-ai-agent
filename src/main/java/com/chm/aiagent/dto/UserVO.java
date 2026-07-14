package com.chm.aiagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private String id;
    private String username;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
