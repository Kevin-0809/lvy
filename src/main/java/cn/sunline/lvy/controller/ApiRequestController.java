package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiRequestDTO;
import cn.sunline.lvy.dto.ApiRequestRequest;
import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.service.ApiRequestService;
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
 * 已保存请求接口。
 */
@RestController
@RequestMapping("/api")
public class ApiRequestController {

    private final ApiRequestService requestService;

    public ApiRequestController(ApiRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/collections/{collectionId}/requests")
    public ApiResponse<List<ApiRequestDTO>> listByCollection(@PathVariable Long collectionId) {
        return ApiResponse.ok(requestService.listByCollection(collectionId));
    }

    @PostMapping("/requests")
    public ApiResponse<ApiRequestDTO> create(@Valid @RequestBody ApiRequestRequest request) {
        return ApiResponse.ok(requestService.create(request));
    }

    @PutMapping("/requests/{id}")
    public ApiResponse<ApiRequestDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody ApiRequestRequest request) {
        return ApiResponse.ok(requestService.update(id, request));
    }

    @DeleteMapping("/requests/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requestService.delete(id);
        return ApiResponse.ok();
    }
}
