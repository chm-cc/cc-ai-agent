package com.chm.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotBlank(message = "agentId 不能为空")
    private String agentId;
    private String title;
}
