package com.chm.aiagent.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

    private final JdbcTemplate jdbc;

    // ========== 总览 ==========

    public int countTotalConversations() {
        return jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class);
    }

    public int countTotalMessages() {
        Integer sum = jdbc.queryForObject(
            "SELECT COALESCE(sum(message_count), 0) FROM conversations", Integer.class);
        return sum != null ? sum : 0;
    }

    public int countTodayConversations() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM conversations WHERE created_at::date = CURRENT_DATE", Integer.class);
    }

    public int countTodayMessages() {
        Integer sum = jdbc.queryForObject(
            "SELECT COALESCE(sum(message_count), 0) FROM conversations WHERE updated_at::date = CURRENT_DATE",
            Integer.class);
        return sum != null ? sum : 0;
    }

    // ========== Agent 分解 ==========

    public List<Map<String, Object>> agentBreakdown() {
        return jdbc.queryForList(
            "SELECT agent_id, count(*) as conversations, " +
            "COALESCE(sum(message_count), 0) as messages, " +
            "COALESCE(avg(message_count), 0) as avg_per_conv " +
            "FROM conversations GROUP BY agent_id ORDER BY conversations DESC");
    }

    // ========== 每日趋势 ==========

    public List<Map<String, Object>> dailyConversations(LocalDate since) {
        return jdbc.queryForList(
            "SELECT created_at::date as day, count(*) as cnt " +
            "FROM conversations WHERE created_at::date >= ? " +
            "GROUP BY day ORDER BY day", since);
    }

    public List<Map<String, Object>> dailyMessages(LocalDate since) {
        return jdbc.queryForList(
            "SELECT created_at::date as day, count(*) as cnt " +
            "FROM messages WHERE created_at::date >= ? " +
            "GROUP BY day ORDER BY day", since);
    }
}
