package com.chm.aiagent.repository;

import com.chm.aiagent.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Message> rowMapper = (rs, rowNum) -> {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setConversationId(rs.getString("conversation_id"));
        m.setRole(rs.getString("role"));
        m.setContent(rs.getString("content"));
        m.setFeedback(rs.getString("feedback"));
        m.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return m;
    };

    public void insert(Message m) {
        jdbc.update(
            "INSERT INTO messages (conversation_id, role, content) VALUES (?,?,?)",
            m.getConversationId(), m.getRole(), m.getContent());
    }

    public List<Message> findByConversation(String conversationId, int offset, int limit) {
        return jdbc.query(
            "SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at ASC LIMIT ? OFFSET ?",
            rowMapper, conversationId, limit, offset);
    }

    public int countByConversation(String conversationId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM messages WHERE conversation_id = ?", Integer.class, conversationId);
    }

    public void updateFeedback(Long msgId, String feedback) {
        jdbc.update("UPDATE messages SET feedback = ? WHERE id = ?", feedback, msgId);
    }
}
