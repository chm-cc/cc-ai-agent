package com.chm.aiagent.repository;

import com.chm.aiagent.model.Conversation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Conversation> rowMapper = (rs, rowNum) -> {
        Conversation c = new Conversation();
        c.setId(rs.getString("id"));
        c.setAgentId(rs.getString("agent_id"));
        c.setUserId(rs.getString("user_id"));
        c.setTitle(rs.getString("title"));
        c.setLastMessage(rs.getString("last_message"));
        c.setMessageCount(rs.getInt("message_count"));
        c.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        c.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return c;
    };

    public void insert(Conversation c) {
        jdbc.update(
            "INSERT INTO conversations (id, agent_id, user_id, title, created_at, updated_at) VALUES (?,?,?,?,?,?)",
            c.getId(), c.getAgentId(), c.getUserId(), c.getTitle(),
            Timestamp.valueOf(c.getCreatedAt()), Timestamp.valueOf(c.getUpdatedAt())
        );
    }

    public Conversation findById(String id) {
        List<Conversation> list = jdbc.query(
            "SELECT * FROM conversations WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Conversation> findByUser(String userId, String agentId, int offset, int limit) {
        return jdbc.query(
            "SELECT * FROM conversations WHERE user_id = ? AND agent_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
            rowMapper, userId, agentId, limit, offset);
    }

    public int countByUser(String userId, String agentId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM conversations WHERE user_id = ? AND agent_id = ?",
            Integer.class, userId, agentId);
    }

    public void updateAfterMessage(String id, String lastMessage, LocalDateTime updatedAt) {
        jdbc.update(
            "UPDATE conversations SET last_message = ?, message_count = message_count + 1, updated_at = ? WHERE id = ?",
            lastMessage, Timestamp.valueOf(updatedAt), id);
    }

    public void deleteById(String id) {
        jdbc.update("DELETE FROM conversations WHERE id = ?", id);
    }

    public void rename(String id, String title) {
        jdbc.update("UPDATE conversations SET title = ?, updated_at = now() WHERE id = ?", title, id);
    }

    public int deleteEmptyByUserAndAgent(String userId, String agentId) {
        return jdbc.update(
            "DELETE FROM conversations WHERE user_id = ? AND agent_id = ? AND message_count = 0",
            userId, agentId);
    }
}
