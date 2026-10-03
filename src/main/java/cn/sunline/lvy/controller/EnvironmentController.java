package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.EnvironmentDTO;
import cn.sunline.lvy.dto.EnvironmentRequest;
import cn.sunline.lvy.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 环境变量接口。
 */
@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping
    public ApiResponse<List<EnvironmentDTO>> list() {
        return ApiResponse.ok(environmentService.list());
    }

    @PostMapping
    public ApiResponse<EnvironmentDTO> create(@Valid @RequestBody EnvironmentRequest request) {
        return ApiResponse.ok(environmentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EnvironmentDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody EnvironmentRequest request) {
        return ApiResponse.ok(environmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        environmentService.delete(id);
        return ApiResponse.ok();
    }
}
