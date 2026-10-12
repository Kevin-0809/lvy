package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.RequestHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请求历史读写，基于 JdbcTemplate。
 */
@Repository
public class RequestHistoryRepository {

    private static final String COLUMNS = "id, method, url, headers, params, body_type, body_content, "
            + "auth_type, auth_config, status_code, duration_ms, response_size, created_at";

    private static final RowMapper<RequestHistory> MAPPER = (rs, rowNum) -> {
        RequestHistory history = new RequestHistory();
        history.setId(rs.getLong("id"));
        history.setMethod(rs.getString("method"));
        history.setUrl(rs.getString("url"));
        history.setHeaders(rs.getString("headers"));
        history.setParams(rs.getString("params"));
        history.setBodyType(rs.getString("body_type"));
        history.setBodyContent(rs.getString("body_content"));
        history.setAuthType(rs.getString("auth_type"));
        history.setAuthConfig(rs.getString("auth_config"));
        int statusCode = rs.getInt("status_code");
        history.setStatusCode(rs.wasNull() ? null : statusCode);
        long durationMs = rs.getLong("duration_ms");
        history.setDurationMs(rs.wasNull() ? null : durationMs);
        long responseSize = rs.getLong("response_size");
        history.setResponseSize(rs.wasNull() ? null : responseSize);
        history.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return history;
    };

    private final JdbcTemplate jdbcTemplate;

    public RequestHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RequestHistory save(RequestHistory history) {
        LocalDateTime now = LocalDateTime.now();
        history.setCreatedAt(now);
        Long id = jdbcTemplate.queryForObject(
                "insert into ivy_request_history(method, url, headers, params, body_type, body_content, "
                        + "auth_type, auth_config, status_code, duration_ms, response_size, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                Long.class, history.getMethod(), history.getUrl(), history.getHeaders(), history.getParams(),
                history.getBodyType(), history.getBodyContent(), history.getAuthType(), history.getAuthConfig(),
                history.getStatusCode(), history.getDurationMs(), history.getResponseSize(), now);
        history.setId(id);
        return history;
    }

    public List<RequestHistory> page(int offset, int limit) {
        return jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_request_history order by created_at desc limit ? offset ?",
                MAPPER, limit, offset);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from ivy_request_history", Long.class);
        return count == null ? 0L : count;
    }

    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ivy_request_history where id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("delete from ivy_request_history where id = ?", id);
    }

    public void deleteAll() {
        jdbcTemplate.update("delete from ivy_request_history");
    }
}
