package com.chm.aiagent.dto;

import lombok.Data;
import java.util.List;

@Data
public class StatisticsVO {
    private Overview overview;
    private List<AgentBreakdown> agentBreakdown;
    private List<DailyTrend> dailyTrend;

    @Data
    public static class Overview {
        private int totalConversations;
        private int totalMessages;
        private int todayConversations;
        private int todayMessages;
    }

    @Data
    public static class AgentBreakdown {
        private String agentId;
        private String agentName;
        private int conversations;
        private int messages;
        private double avgPerConv;
    }

    @Data
    public static class DailyTrend {
        private String date;
        private int conversations;
        private int messages;
    }
}
