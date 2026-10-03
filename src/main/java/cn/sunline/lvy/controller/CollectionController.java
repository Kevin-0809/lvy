package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.CollectionDTO;
import cn.sunline.lvy.dto.CollectionRequest;
import cn.sunline.lvy.service.CollectionService;
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
 * 集合接口。
 */
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ApiResponse<List<CollectionDTO>> list() {
        return ApiResponse.ok(collectionService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<CollectionDTO> get(@PathVariable Long id) {
        return ApiResponse.ok(collectionService.get(id));
    }

    @PostMapping
    public ApiResponse<CollectionDTO> create(@Valid @RequestBody CollectionRequest request) {
        return ApiResponse.ok(collectionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CollectionDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody CollectionRequest request) {
        return ApiResponse.ok(collectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        collectionService.delete(id);
        return ApiResponse.ok();
    }
}
