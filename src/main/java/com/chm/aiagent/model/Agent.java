package com.chm.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    private String id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String tags;          // JSON array string
    private String systemPrompt;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String tools;         // JSON array string
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
