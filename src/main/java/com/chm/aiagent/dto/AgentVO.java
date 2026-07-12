package com.chm.aiagent.dto;

import lombok.Data;
import java.util.List;

@Data
public class AgentVO {
    private String id;
    private String name;
    private String description;
    private String icon;
    private String category;
    private List<String> tags;
    private String status;
}
