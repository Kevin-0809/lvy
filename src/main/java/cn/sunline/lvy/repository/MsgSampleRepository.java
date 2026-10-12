package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.MsgSample;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 本地报文样本读写，基于 JdbcTemplate。
 */
@Repository
public class MsgSampleRepository {

    private static final String COLUMNS = "id, service_code, partition_name, trans_id, source_ip, txn_time, "
            + "message_type, global_seq_no, tran_teller_no, content, edited, created_at, updated_at";

    private static final RowMapper<MsgSample> MAPPER = (rs, rowNum) -> {
        MsgSample sample = new MsgSample();
        sample.setId(rs.getLong("id"));
        sample.setServiceCode(rs.getString("service_code"));
        sample.setPartitionName(rs.getString("partition_name"));
        sample.setTransId(rs.getString("trans_id"));
        sample.setSourceIp(rs.getString("source_ip"));
        long txnTime = rs.getLong("txn_time");
        sample.setTxnTime(rs.wasNull() ? null : txnTime);
        sample.setMessageType(rs.getString("message_type"));
        sample.setGlobalSeqNo(rs.getString("global_seq_no"));
        sample.setTranTellerNo(rs.getString("tran_teller_no"));
        sample.setContent(rs.getString("content"));
        sample.setEdited(rs.getBoolean("edited"));
        sample.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        sample.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return sample;
    };

    private final JdbcTemplate jdbcTemplate;

    public MsgSampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByTransIdAndSourceIpAndMessageType(String transId, String sourceIp, String messageType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from ivy_msg_sample "
                        + "where trans_id = ? and source_ip = ? and message_type = ?",
                Integer.class, transId, sourceIp, messageType);
        return count != null && count > 0;
    }

    public Optional<MsgSample> findById(Long id) {
        List<MsgSample> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_msg_sample where id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public MsgSample save(MsgSample sample) {
        LocalDateTime now = LocalDateTime.now();
        if (sample.getId() == null) {
            sample.setCreatedAt(now);
            sample.setUpdatedAt(now);
            Long id = jdbcTemplate.queryForObject(
                    "insert into ivy_msg_sample(service_code, partition_name, trans_id, source_ip, txn_time, "
                            + "message_type, global_seq_no, tran_teller_no, content, edited, created_at, updated_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                    Long.class, sample.getServiceCode(), sample.getPartitionName(), sample.getTransId(),
                    sample.getSourceIp(), sample.getTxnTime(), sample.getMessageType(), sample.getGlobalSeqNo(),
                    sample.getTranTellerNo(), sample.getContent(), sample.isEdited(), now, now);
            sample.setId(id);
        } else {
            sample.setUpdatedAt(now);
            jdbcTemplate.update(
                    "update ivy_msg_sample set service_code = ?, partition_name = ?, trans_id = ?, source_ip = ?, "
                            + "txn_time = ?, message_type = ?, global_seq_no = ?, tran_teller_no = ?, content = ?, "
                            + "edited = ?, updated_at = ? where id = ?",
                    sample.getServiceCode(), sample.getPartitionName(), sample.getTransId(), sample.getSourceIp(),
                    sample.getTxnTime(), sample.getMessageType(), sample.getGlobalSeqNo(), sample.getTranTellerNo(),
                    sample.getContent(), sample.isEdited(), now, sample.getId());
        }
        return sample;
    }

    public List<MsgSample> page(String keyword, int offset, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return jdbcTemplate.query(
                    "select " + COLUMNS + " from ivy_msg_sample order by created_at desc limit ? offset ?",
                    MAPPER, limit, offset);
        }
        String like = "%" + keyword.trim() + "%";
        return jdbcTemplate.query(
                "select " + COLUMNS + " from ivy_msg_sample "
                        + "where service_code like ? or trans_id like ? order by created_at desc limit ? offset ?",
                MAPPER, like, like, limit, offset);
    }

    public long count(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            Long count = jdbcTemplate.queryForObject("select count(*) from ivy_msg_sample", Long.class);
            return count == null ? 0L : count;
        }
        String like = "%" + keyword.trim() + "%";
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from ivy_msg_sample where service_code like ? or trans_id like ?",
                Long.class, like, like);
        return count == null ? 0L : count;
    }
}
