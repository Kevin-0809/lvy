package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.ApiRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 已保存请求读写，基于 JdbcTemplate。
 */
@Repository
public class ApiRequestRepository {

    private static final String COLUMNS = "id, collection_id, name, method, url, headers, params, "
            + "body_type, body_content, auth_type, auth_config, created_at, updated_at";

    private static final RowMapper<ApiRequest> MAPPER = (rs, rowNum) -> {
        ApiRequest request = new ApiRequest();
        request.setId(rs.getLong("id"));
        request.setCollectionId(rs.getLong("collection_id"));
        request.setName(rs.getString("name"));
        request.setMethod(rs.getString("method"));
        request.setUrl(rs.getString("url"));
        request.setHeaders(rs.getString("headers"));
        request.setParams(rs.getString("params"));
        request.setBodyType(rs.getString("body_type"));
        request.setBodyContent(rs.getString("body_content"));
        request.setAuthType(rs.getString("auth_type"));
        request.setAuthConfig(rs.getString("auth_config"));
        request.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        request.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return request;
    };

    private final JdbcTemplate jdbcTemplate;

    public ApiRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApiRequest> findByCollectionIdOrderByUpdatedAtDesc(Long collectionId) {
        return jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_api_request where collection_id = ? order by updated_at desc",
                MAPPER, collectionId);
    }

    public long countByCollectionId(Long collectionId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from ivy_api_request where collection_id = ?", Long.class, collectionId);
        return count == null ? 0L : count;
    }

    public Optional<ApiRequest> findById(Long id) {
        List<ApiRequest> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_api_request where id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public ApiRequest save(ApiRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.getId() == null) {
            request.setCreatedAt(now);
            request.setUpdatedAt(now);
            Long id = jdbcTemplate.queryForObject(
                    "insert into ivy_api_request(collection_id, name, method, url, headers, params, "
                            + "body_type, body_content, auth_type, auth_config, created_at, updated_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                    Long.class, request.getCollectionId(), request.getName(), request.getMethod(),
                    request.getUrl(), request.getHeaders(), request.getParams(), request.getBodyType(),
                    request.getBodyContent(), request.getAuthType(), request.getAuthConfig(), now, now);
            request.setId(id);
        } else {
            request.setUpdatedAt(now);
            jdbcTemplate.update(
                    "update ivy_api_request set collection_id = ?, name = ?, method = ?, url = ?, headers = ?, "
                            + "params = ?, body_type = ?, body_content = ?, auth_type = ?, auth_config = ?, "
                            + "updated_at = ? where id = ?",
                    request.getCollectionId(), request.getName(), request.getMethod(), request.getUrl(),
                    request.getHeaders(), request.getParams(), request.getBodyType(), request.getBodyContent(),
                    request.getAuthType(), request.getAuthConfig(), now, request.getId());
        }
        return request;
    }

    public void deleteByCollectionId(Long collectionId) {
        jdbcTemplate.update("delete from ivy_api_request where collection_id = ?", collectionId);
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("delete from ivy_api_request where id = ?", id);
    }
}
