package com.chm.aiagent.repository;

import com.chm.aiagent.model.Agent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Agent> rowMapper = (rs, rowNum) -> {
        Agent a = new Agent();
        a.setId(rs.getString("id"));
        a.setName(rs.getString("name"));
        a.setDescription(rs.getString("description"));
        a.setIcon(rs.getString("icon"));
        a.setCategory(rs.getString("category"));
        a.setTags(rs.getString("tags"));
        a.setSystemPrompt(rs.getString("system_prompt"));
        a.setModel(rs.getString("model"));
        a.setTemperature(rs.getObject("temperature", Double.class));
        a.setMaxTokens(rs.getObject("max_tokens", Integer.class));
        a.setTools(rs.getString("tools"));
        a.setStatus(rs.getString("status"));
        a.setSortOrder(rs.getObject("sort_order", Integer.class));
        a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        a.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return a;
    };

    public List<Agent> findAll() {
        return jdbc.query("SELECT * FROM agents ORDER BY sort_order ASC, created_at ASC", rowMapper);
    }

    public List<Agent> findActive() {
        return jdbc.query("SELECT * FROM agents WHERE status = 'ACTIVE' ORDER BY sort_order ASC, created_at ASC", rowMapper);
    }

    public Agent findById(String id) {
        List<Agent> list = jdbc.query("SELECT * FROM agents WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public int count() {
        return jdbc.queryForObject("SELECT count(*) FROM agents", Integer.class);
    }

    public void insert(Agent a) {
        jdbc.update(
            "INSERT INTO agents (id, name, description, icon, category, tags, system_prompt, model, temperature, max_tokens, tools, status, sort_order, created_at, updated_at) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            a.getId(), a.getName(), a.getDescription(), a.getIcon(), a.getCategory(),
            a.getTags(), a.getSystemPrompt(), a.getModel(), a.getTemperature(), a.getMaxTokens(),
            a.getTools(), a.getStatus(), a.getSortOrder(),
            Timestamp.valueOf(a.getCreatedAt()), Timestamp.valueOf(a.getUpdatedAt())
        );
    }

    public void update(Agent a) {
        jdbc.update(
            "UPDATE agents SET name=?, description=?, icon=?, category=?, tags=?, system_prompt=?, " +
            "model=?, temperature=?, max_tokens=?, tools=?, status=?, sort_order=?, updated_at=? WHERE id=?",
            a.getName(), a.getDescription(), a.getIcon(), a.getCategory(),
            a.getTags(), a.getSystemPrompt(), a.getModel(), a.getTemperature(), a.getMaxTokens(),
            a.getTools(), a.getStatus(), a.getSortOrder(),
            Timestamp.valueOf(a.getUpdatedAt()), a.getId()
        );
    }

    public void deleteById(String id) {
        jdbc.update("DELETE FROM agents WHERE id = ?", id);
    }
}
