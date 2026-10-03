package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.HistoryDTO;
import cn.sunline.lvy.dto.PageResult;
import cn.sunline.lvy.service.HistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史记录接口。
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ApiResponse<PageResult<HistoryDTO>> page(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(historyService.page(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        historyService.delete(id);
        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> clear() {
        historyService.clear();
        return ApiResponse.ok();
    }
}
