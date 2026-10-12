package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.HistoryDTO;
import cn.sunline.lvy.dto.PageResult;
import cn.sunline.lvy.dto.ProxyRequest;
import cn.sunline.lvy.dto.ProxyResponse;
import cn.sunline.lvy.entity.RequestHistory;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.RequestHistoryRepository;
import cn.sunline.lvy.util.JsonUtil;
import cn.sunline.lvy.util.SensitiveData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 请求历史服务。
 */
@Service
public class HistoryService {

    private final RequestHistoryRepository historyRepository;

    public HistoryService(RequestHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void record(ProxyRequest request, ProxyResponse response) {
        RequestHistory history = new RequestHistory();
        history.setMethod(request.getMethod());
        history.setUrl(request.getUrl());
        history.setHeaders(JsonUtil.toJson(request.getHeaders()));
        history.setParams(JsonUtil.toJson(request.getParams()));
        history.setBodyType(request.getBodyType());
        history.setBodyContent(request.getBodyContent());
        history.setAuthType(request.getAuthType());
        history.setAuthConfig(JsonUtil.toJson(request.getAuthConfig()));
        if (response != null) {
            history.setStatusCode(response.getStatusCode());
            history.setDurationMs(response.getDurationMs());
            history.setResponseSize(response.getSize());
        }
        historyRepository.save(history);
    }

    public PageResult<HistoryDTO> page(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        long total = historyRepository.count();
        List<HistoryDTO> items = historyRepository.page(safePage * safeSize, safeSize).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new PageResult<>(items, total, safePage, safeSize);
    }

    @Transactional
    public void delete(Long id) {
        if (!historyRepository.existsById(id)) {
            throw new BizException(404, "历史记录不存在: " + id);
        }
        historyRepository.deleteById(id);
    }

    @Transactional
    public void clear() {
        historyRepository.deleteAll();
    }

    private HistoryDTO toDto(RequestHistory entity) {
        HistoryDTO dto = new HistoryDTO();
        dto.setId(entity.getId());
        dto.setMethod(entity.getMethod());
        dto.setUrl(entity.getUrl());
        dto.setHeaders(JsonUtil.parseKeyValues(entity.getHeaders()));
        dto.setParams(JsonUtil.parseKeyValues(entity.getParams()));
        dto.setBodyType(entity.getBodyType());
        dto.setBodyContent(entity.getBodyContent());
        dto.setAuthType(entity.getAuthType());
        dto.setAuthConfig(SensitiveData.maskAuthConfig(JsonUtil.parseStringMap(entity.getAuthConfig())));
        dto.setStatusCode(entity.getStatusCode());
        dto.setDurationMs(entity.getDurationMs());
        dto.setResponseSize(entity.getResponseSize());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
