package com.chm.aiagent.config;

import com.chm.aiagent.dto.AgentVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 种子数据配置，仅用于首次启动时导入 agents 表。
 * 运行时 Agent 数据统一走 AgentService → AgentRepository → agents 表。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class AgentConfig {
    private List<AgentVO> agents = new ArrayList<>();
}
