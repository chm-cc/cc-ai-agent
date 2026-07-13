package com.chm.aiagent.repository;

import com.chm.aiagent.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setEnabled(rs.getBoolean("enabled"));
        u.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        u.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return u;
    };

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query(
                "SELECT * FROM users WHERE username = ? AND enabled = true", rowMapper, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void insert(User user) {
        jdbc.update(
                "INSERT INTO users (id, username, password, role, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?)",
                user.getId(), user.getUsername(), user.getPassword(), user.getRole(),
                user.isEnabled(),
                user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()),
                user.getUpdatedAt() != null ? Timestamp.valueOf(user.getUpdatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }
}
