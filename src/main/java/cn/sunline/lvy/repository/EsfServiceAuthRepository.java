package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.EsfServiceAuth;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * tss_service_auth 读写，基于 JdbcTemplate。该表为 TSS 既有表，由外部维护。
 */
@Repository
public class EsfServiceAuthRepository {

    private static final String COLUMNS = "protocol_id, sid, gk, wk, pk, create_time, update_time";

    private static final RowMapper<EsfServiceAuth> MAPPER = (rs, rowNum) -> {
        EsfServiceAuth auth = new EsfServiceAuth();
        auth.setProtocolId(rs.getString("protocol_id"));
        auth.setSid(rs.getString("sid"));
        auth.setGk(rs.getString("gk"));
        auth.setWk(rs.getString("wk"));
        auth.setPk(rs.getString("pk"));
        auth.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
        auth.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
        return auth;
    };

    private final JdbcTemplate jdbcTemplate;

    public EsfServiceAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from tss_service_auth", Long.class);
        return count == null ? 0L : count;
    }

    public Optional<EsfServiceAuth> findById(String protocolId) {
        List<EsfServiceAuth> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from tss_service_auth where protocol_id = ?", MAPPER, protocolId);
        return rows.stream().findFirst();
    }

    public void saveAll(List<EsfServiceAuth> rows) {
        LocalDateTime now = LocalDateTime.now();
        for (EsfServiceAuth auth : rows) {
            auth.setCreateTime(now);
            auth.setUpdateTime(now);
            jdbcTemplate.update(
                    "insert into tss_service_auth(protocol_id, sid, gk, wk, pk, create_time, update_time) "
                            + "values (?, ?, ?, ?, ?, ?, ?)",
                    auth.getProtocolId(), auth.getSid(), auth.getGk(), auth.getWk(), auth.getPk(), now, now);
        }
    }
}
