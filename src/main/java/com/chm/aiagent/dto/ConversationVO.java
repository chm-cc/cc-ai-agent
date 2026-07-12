package com.chm.aiagent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private String id;
    private String agentId;
    private String title;
    private String lastMessage;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
