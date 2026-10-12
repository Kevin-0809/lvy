package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.ApiRequestDTO;
import cn.sunline.lvy.dto.ApiRequestRequest;
import cn.sunline.lvy.entity.ApiRequest;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.ApiCollectionRepository;
import cn.sunline.lvy.repository.ApiRequestRepository;
import cn.sunline.lvy.util.JsonUtil;
import cn.sunline.lvy.util.SensitiveData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 已保存请求服务。
 */
@Service
public class ApiRequestService {

    private final ApiRequestRepository requestRepository;
    private final ApiCollectionRepository collectionRepository;

    public ApiRequestService(ApiRequestRepository requestRepository,
                             ApiCollectionRepository collectionRepository) {
        this.requestRepository = requestRepository;
        this.collectionRepository = collectionRepository;
    }

    public List<ApiRequestDTO> listByCollection(Long collectionId) {
        if (!collectionRepository.existsById(collectionId)) {
            throw new BizException(404, "集合不存在: " + collectionId);
        }
        return requestRepository.findByCollectionIdOrderByUpdatedAtDesc(collectionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiRequestDTO create(ApiRequestRequest request) {
        if (request.getCollectionId() == null) {
            throw new BizException(400, "collectionId 不能为空");
        }
        if (!collectionRepository.existsById(request.getCollectionId())) {
            throw new BizException(404, "集合不存在: " + request.getCollectionId());
        }
        ApiRequest entity = new ApiRequest();
        entity.setCollectionId(request.getCollectionId());
        apply(entity, request);
        return toDto(requestRepository.save(entity));
    }

    @Transactional
    public ApiRequestDTO update(Long id, ApiRequestRequest request) {
        ApiRequest entity = find(id);
        if (request.getCollectionId() != null) {
            if (!collectionRepository.existsById(request.getCollectionId())) {
                throw new BizException(404, "集合不存在: " + request.getCollectionId());
            }
            entity.setCollectionId(request.getCollectionId());
        }
        apply(entity, request);
        return toDto(requestRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        find(id);
        requestRepository.deleteById(id);
    }

    private ApiRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "请求不存在: " + id));
    }

    private void apply(ApiRequest entity, ApiRequestRequest request) {
        entity.setName(request.getName() == null ? null : request.getName().trim());
        entity.setMethod(request.getMethod());
        entity.setUrl(request.getUrl());
        entity.setHeaders(JsonUtil.toJson(request.getHeaders()));
        entity.setParams(JsonUtil.toJson(request.getParams()));
        entity.setBodyType(request.getBodyType());
        entity.setBodyContent(request.getBodyContent());
        entity.setAuthType(request.getAuthType());
        entity.setAuthConfig(JsonUtil.toJson(request.getAuthConfig()));
    }

    private ApiRequestDTO toDto(ApiRequest entity) {
        ApiRequestDTO dto = new ApiRequestDTO();
        dto.setId(entity.getId());
        dto.setCollectionId(entity.getCollectionId());
        dto.setName(entity.getName());
        dto.setMethod(entity.getMethod());
        dto.setUrl(entity.getUrl());
        dto.setHeaders(JsonUtil.parseKeyValues(entity.getHeaders()));
        dto.setParams(JsonUtil.parseKeyValues(entity.getParams()));
        dto.setBodyType(entity.getBodyType());
        dto.setBodyContent(entity.getBodyContent());
        dto.setAuthType(entity.getAuthType());
        dto.setAuthConfig(SensitiveData.maskAuthConfig(JsonUtil.parseStringMap(entity.getAuthConfig())));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
