package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.EnvironmentDTO;
import cn.sunline.lvy.dto.EnvironmentRequest;
import cn.sunline.lvy.entity.Environment;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.EnvironmentRepository;
import cn.sunline.lvy.util.JsonUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 环境变量服务。
 */
@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    public List<EnvironmentDTO> list() {
        return environmentRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnvironmentDTO create(EnvironmentRequest request) {
        Environment entity = new Environment();
        entity.setName(request.getName().trim());
        entity.setVariables(JsonUtil.toJson(request.getVariables()));
        return toDto(environmentRepository.save(entity));
    }

    @Transactional
    public EnvironmentDTO update(Long id, EnvironmentRequest request) {
        Environment entity = find(id);
        entity.setName(request.getName().trim());
        entity.setVariables(JsonUtil.toJson(request.getVariables()));
        return toDto(environmentRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Environment entity = find(id);
        environmentRepository.delete(entity);
    }

    private Environment find(Long id) {
        return environmentRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "环境不存在: " + id));
    }

    private EnvironmentDTO toDto(Environment entity) {
        EnvironmentDTO dto = new EnvironmentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setVariables(JsonUtil.parseStringMap(entity.getVariables()));
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
