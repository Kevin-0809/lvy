package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.ApiCollection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 集合读写，基于 JdbcTemplate。
 */
@Repository
public class ApiCollectionRepository {

    private static final String COLUMNS = "id, name, description, created_at, updated_at";

    private static final RowMapper<ApiCollection> MAPPER = (rs, rowNum) -> {
        ApiCollection collection = new ApiCollection();
        collection.setId(rs.getLong("id"));
        collection.setName(rs.getString("name"));
        collection.setDescription(rs.getString("description"));
        collection.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        collection.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return collection;
    };

    private final JdbcTemplate jdbcTemplate;

    public ApiCollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApiCollection> findAllByOrderByUpdatedAtDesc() {
        return jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_collection order by updated_at desc", MAPPER);
    }

    public Optional<ApiCollection> findById(Long id) {
        List<ApiCollection> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_collection where id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ivy_collection where id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public ApiCollection save(ApiCollection collection) {
        LocalDateTime now = LocalDateTime.now();
        if (collection.getId() == null) {
            collection.setCreatedAt(now);
            collection.setUpdatedAt(now);
            Long id = jdbcTemplate.queryForObject(
                    "insert into ivy_collection(name, description, created_at, updated_at) "
                            + "values (?, ?, ?, ?) returning id",
                    Long.class, collection.getName(), collection.getDescription(), now, now);
            collection.setId(id);
        } else {
            collection.setUpdatedAt(now);
            jdbcTemplate.update(
                    "update ivy_collection set name = ?, description = ?, updated_at = ? where id = ?",
                    collection.getName(), collection.getDescription(), now, collection.getId());
        }
        return collection;
    }

    public void delete(ApiCollection collection) {
        if (collection != null && collection.getId() != null) {
            jdbcTemplate.update("delete from ivy_collection where id = ?", collection.getId());
        }
    }
}
