package cn.sunline.lvy.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于 openGauss（PostgreSQL 协议）的 bxds 报文源实现。
 *
 * <p>分区信息来自当前连接默认 schema 下的 {@code pg_partition}，分区名规则为 {@code p{yyyyMMdd}}，
 * 与 rose 项目中的分区访问保持一致。
 *
 * <p>源表 {@code txn_code} 存的是「服务码&格式」（如
 * {@code S30759418FcySetlRateInq&bzjson}），因此输入纯服务码时自动补全。
 * {@code request_message} 为二进制 blob，这里通过 {@link ResultSet#getBytes}
 * 取原始字节，若驱动返回的是十六进制文本再做一次 hex 解码。
 */
@Component
public class JdbcBxdsMsgSource implements MsgSourceReader {

    private static final String REQUEST_TABLE = "msg_flow_log_request";
    private static final List<String> MESSAGE_TYPES = List.of("bzjson", "soap");
    private static final Pattern PARTITION = Pattern.compile("^p[0-9]{8}$");

    private final NamedParameterJdbcTemplate bxdsJdbcTemplate;

    public JdbcBxdsMsgSource(@Qualifier("bxdsJdbcTemplate") NamedParameterJdbcTemplate bxdsJdbcTemplate) {
        this.bxdsJdbcTemplate = bxdsJdbcTemplate;
    }

    @Override
    public List<String> listPartitionsDesc() {
        return bxdsJdbcTemplate.queryForList("""
                select part.relname
                from pg_partition part
                join pg_class parent on parent.oid = part.parentid
                join pg_namespace ns on ns.oid = parent.relnamespace
                where ns.nspname = current_schema()
                  and parent.relname = :table
                  and part.parttype = 'p'
                  and part.relname ~ '^p[0-9]{8}$'
                order by part.relname desc
                """, new MapSqlParameterSource()
                .addValue("table", REQUEST_TABLE), String.class);
    }

    @Override
    public List<MsgSourceRecord> queryPartition(String partitionName, String serviceCode,
                                                String messageType, int limit) {
        if (partitionName == null || !PARTITION.matcher(partitionName).matches()) {
            throw new IllegalArgumentException("非法分区名: " + partitionName);
        }
        if (messageType == null || !MESSAGE_TYPES.contains(messageType)) {
            throw new IllegalArgumentException("不支持的报文类型: " + messageType);
        }
        String sql = "select source_ip, trans_id, txn_time, message_type, request_message, "
                + "global_seq_no, tran_teller_no "
                + "from " + REQUEST_TABLE
                + " partition(" + partitionName + ") "
                + "where txn_code = :code and message_type = :type "
                + "order by txn_time desc "
                + "limit :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", resolveTxnCode(serviceCode, messageType))
                .addValue("type", messageType)
                .addValue("limit", limit);
        return bxdsJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    /**
     * 源表 txn_code 存的是「服务码&格式」（如 {@code S30759418FcySetlRateInq&bzjson}），
     * 输入纯服务码时按类型补全；若输入已带 {@code &}，则按完整值精确匹配。
     */
    private String resolveTxnCode(String serviceCode, String messageType) {
        String value = serviceCode == null ? "" : serviceCode.trim();
        if (value.contains("&")) {
            return value;
        }
        return value + "&" + messageType;
    }

    private static final RowMapper<MsgSourceRecord> ROW_MAPPER = (rs, rowNum) -> {
        long txnTimeValue = rs.getLong("txn_time");
        Long txnTime = rs.wasNull() ? null : txnTimeValue;
        return new MsgSourceRecord(
                rs.getString("trans_id"),
                rs.getString("source_ip"),
                rs.getString("message_type"),
                txnTime,
                rs.getString("global_seq_no"),
                rs.getString("tran_teller_no"),
                normalize(rs.getBytes("request_message")));
    };

    /**
     * openGauss JDBC 可能把 blob 以十六进制文本返回；这里识别纯 hex 字节并解码回原始字节。
     * 真实报文体（JSON/XML 文本）包含非 hex 字符，不会被误判。
     */
    private static byte[] normalize(byte[] raw) {
        if (raw == null || raw.length < 2 || raw.length % 2 != 0) {
            return raw == null ? new byte[0] : raw;
        }
        for (byte b : raw) {
            if (!isHexChar(b)) {
                return raw;
            }
        }
        return hexToBytes(raw);
    }

    private static boolean isHexChar(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }

    private static byte[] hexToBytes(byte[] hex) {
        byte[] result = new byte[hex.length / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }
}
