package com.chm.aiagent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
