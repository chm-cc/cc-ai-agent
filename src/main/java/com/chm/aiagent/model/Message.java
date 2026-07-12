package com.chm.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String conversationId;
    private String role;
    private String content;
    private String feedback;
    private LocalDateTime createdAt;
}
