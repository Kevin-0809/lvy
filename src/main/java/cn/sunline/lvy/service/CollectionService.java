package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.CollectionDTO;
import cn.sunline.lvy.dto.CollectionRequest;
import cn.sunline.lvy.entity.ApiCollection;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.ApiCollectionRepository;
import cn.sunline.lvy.repository.ApiRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 集合服务。
 */
@Service
public class CollectionService {

    private final ApiCollectionRepository collectionRepository;
    private final ApiRequestRepository requestRepository;

    public CollectionService(ApiCollectionRepository collectionRepository,
                             ApiRequestRepository requestRepository) {
        this.collectionRepository = collectionRepository;
        this.requestRepository = requestRepository;
    }

    public List<CollectionDTO> list() {
        return collectionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CollectionDTO get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public CollectionDTO create(CollectionRequest request) {
        ApiCollection entity = new ApiCollection();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        return toDto(collectionRepository.save(entity));
    }

    @Transactional
    public CollectionDTO update(Long id, CollectionRequest request) {
        ApiCollection entity = find(id);
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        return toDto(collectionRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ApiCollection entity = find(id);
        requestRepository.deleteByCollectionId(entity.getId());
        collectionRepository.delete(entity);
    }

    private ApiCollection find(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "集合不存在: " + id));
    }

    private CollectionDTO toDto(ApiCollection entity) {
        CollectionDTO dto = new CollectionDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setRequestCount(requestRepository.countByCollectionId(entity.getId()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
