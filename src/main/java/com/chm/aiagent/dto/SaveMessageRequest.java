package com.chm.aiagent.dto;

import lombok.Data;

@Data
public class SaveMessageRequest {
    private String role;
    private String content;
}
