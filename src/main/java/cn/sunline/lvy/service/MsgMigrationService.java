package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.MsgCollectRequest;
import cn.sunline.lvy.dto.MsgCollectResult;
import cn.sunline.lvy.dto.MsgSampleDTO;
import cn.sunline.lvy.dto.MsgSampleUpdateRequest;
import cn.sunline.lvy.dto.PageResult;
import cn.sunline.lvy.entity.MsgSample;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.MsgSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报文迁移：按服务码从 bxds.msg_flow_log_request 的分区（近到远）采集
 * bzjson / soap 报文明文并落本地 H2；两种类型各自采集指定数量。
 */
@Service
public class MsgMigrationService {

    private static final Logger log = LoggerFactory.getLogger(MsgMigrationService.class);
    private static final List<String> MESSAGE_TYPES = List.of("bzjson", "soap");
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 1000;

    private final MsgSourceReader sourceReader;
    private final MsgSampleRepository repository;
    private final Charset defaultCharset;

    public MsgMigrationService(MsgSourceReader sourceReader,
                               MsgSampleRepository repository,
                               @Value("${ivy.msg-migration.charset:UTF-8}") String charset) {
        this.sourceReader = sourceReader;
        this.repository = repository;
        this.defaultCharset = resolveCharset(charset);
    }

    public MsgCollectResult collect(MsgCollectRequest request) {
        String serviceCode = request.getServiceCode() == null ? "" : request.getServiceCode().trim();
        if (serviceCode.isEmpty()) {
            throw new BizException(400, "服务码不能为空");
        }
        int perTypeLimit = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (perTypeLimit < 1 || perTypeLimit > MAX_LIMIT) {
            throw new BizException(400, "每类采集数量需在 1-" + MAX_LIMIT + " 之间");
        }

        MsgCollectResult result = new MsgCollectResult();
        result.setServiceCode(serviceCode);
        result.setRequested(perTypeLimit);

        List<String> partitions;
        try {
            partitions = sourceReader.listPartitionsDesc();
        } catch (IllegalArgumentException e) {
            throw new BizException(400, e.getMessage());
        } catch (Exception e) {
            throw new BizException(502, "连接 bxds 数据源失败: " + e.getMessage());
        }
        if (partitions == null || partitions.isEmpty()) {
            throw new BizException(404, "未在 bxds 找到 msg_flow_log_request 的按日分区");
        }

        Map<String, Integer> collectedByType = new LinkedHashMap<>();
        for (String type : MESSAGE_TYPES) {
            collectedByType.put(type, 0);
        }

        for (String partition : partitions) {
            if (allReached(collectedByType, perTypeLimit)) {
                break;
            }
            result.setScannedPartitions(result.getScannedPartitions() + 1);
            boolean hit = false;

            for (String messageType : MESSAGE_TYPES) {
                if (collectedByType.get(messageType) >= perTypeLimit) {
                    continue;
                }
                int remaining = perTypeLimit - collectedByType.get(messageType);
                List<MsgSourceRecord> records;
                try {
                    records = sourceReader.queryPartition(partition, serviceCode, messageType, remaining);
                } catch (Exception e) {
                    log.warn("分区 {} 类型 {} 查询失败: {}", partition, messageType, e.getMessage());
                    continue;
                }
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MsgSourceRecord record : records) {
                    if (collectedByType.get(messageType) >= perTypeLimit) {
                        break;
                    }
                    if (isBlank(record.transId()) || isBlank(record.sourceIp())) {
                        continue;
                    }
                    if (repository.existsByTransIdAndSourceIpAndMessageType(
                            record.transId(), record.sourceIp(), messageType)) {
                        result.setSkipped(result.getSkipped() + 1);
                        continue;
                    }
                    try {
                        repository.save(toEntity(serviceCode, partition, record, messageType));
                        collectedByType.put(messageType, collectedByType.get(messageType) + 1);
                        hit = true;
                    } catch (DataIntegrityViolationException e) {
                        result.setSkipped(result.getSkipped() + 1);
                    }
                }
            }
            if (hit) {
                result.getHitPartitions().add(partition);
            }
        }

        int total = collectedByType.values().stream().mapToInt(Integer::intValue).sum();
        result.setCollected(total);
        result.setCollectedByType(collectedByType);
        result.setCompleted(collectedByType.values().stream().allMatch(value -> value >= perTypeLimit));
        return result;
    }

    private boolean allReached(Map<String, Integer> collectedByType, int limit) {
        return collectedByType.values().stream().allMatch(value -> value >= limit);
    }

    @Transactional(readOnly = true)
    public PageResult<MsgSampleDTO> page(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<MsgSample> result = (keyword == null || keyword.isBlank())
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByServiceCodeContainingOrTransIdContainingOrderByCreatedAtDesc(
                        keyword.trim(), keyword.trim(), pageable);
        List<MsgSampleDTO> items = result.getContent().stream()
                .map(sample -> MsgSampleDTO.from(sample, false))
                .toList();
        return new PageResult<>(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public MsgSampleDTO detail(Long id) {
        return MsgSampleDTO.from(find(id), true);
    }

    @Transactional
    public MsgSampleDTO update(Long id, MsgSampleUpdateRequest request) {
        MsgSample sample = find(id);
        sample.setContent(request.getContent());
        sample.setEdited(true);
        return MsgSampleDTO.from(repository.save(sample), true);
    }

    private MsgSample find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BizException(404, "报文不存在: " + id));
    }

    private MsgSample toEntity(String serviceCode, String partition, MsgSourceRecord record, String messageType) {
        MsgSample sample = new MsgSample();
        sample.setServiceCode(serviceCode);
        sample.setPartitionName(partition);
        sample.setTransId(record.transId());
        sample.setSourceIp(record.sourceIp());
        sample.setTxnTime(record.txnTime());
        sample.setMessageType(messageType);
        sample.setGlobalSeqNo(record.globalSeqNo());
        sample.setTranTellerNo(record.tranTellerNo());
        sample.setContent(decode(record.message()));
        sample.setEdited(false);
        return sample;
    }

    private String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String value = decodeStrict(bytes, defaultCharset);
        if (value != null) {
            return value;
        }
        value = decodeStrict(bytes, StandardCharsets.UTF_8);
        if (value != null) {
            return value;
        }
        value = decodeStrict(bytes, Charset.forName("GBK"));
        if (value != null) {
            return value;
        }
        return new String(bytes, defaultCharset);
    }

    private String decodeStrict(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private Charset resolveCharset(String name) {
        if (name == null || name.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(name.trim());
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
