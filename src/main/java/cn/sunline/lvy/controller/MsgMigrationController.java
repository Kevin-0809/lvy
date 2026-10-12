package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.MsgCollectRequest;
import cn.sunline.lvy.dto.MsgCollectTaskDTO;
import cn.sunline.lvy.dto.MsgSampleDTO;
import cn.sunline.lvy.dto.MsgSampleUpdateRequest;
import cn.sunline.lvy.dto.PageResult;
import cn.sunline.lvy.service.MsgCollectTaskService;
import cn.sunline.lvy.service.MsgMigrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报文迁移与本地样本管理接口。
 */
@RestController
@RequestMapping("/api/msg-migration")
public class MsgMigrationController {

    private final MsgMigrationService msgMigrationService;
    private final MsgCollectTaskService msgCollectTaskService;

    public MsgMigrationController(MsgMigrationService msgMigrationService,
                                  MsgCollectTaskService msgCollectTaskService) {
        this.msgMigrationService = msgMigrationService;
        this.msgCollectTaskService = msgCollectTaskService;
    }

    /** 提交异步采集任务，立即返回任务号；多个服务码并发采集。 */
    @PostMapping("/collect")
    public ApiResponse<MsgCollectTaskDTO> collect(@Valid @RequestBody MsgCollectRequest request) {
        return ApiResponse.ok(msgCollectTaskService.submit(request));
    }

    /** 查询采集任务进度。 */
    @GetMapping("/collect/{taskId}")
    public ApiResponse<MsgCollectTaskDTO> collectStatus(@PathVariable String taskId) {
        return ApiResponse.ok(msgCollectTaskService.status(taskId));
    }

    @GetMapping("/samples")
    public ApiResponse<PageResult<MsgSampleDTO>> samples(@RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(msgMigrationService.page(keyword, page, size));
    }

    @GetMapping("/samples/{id}")
    public ApiResponse<MsgSampleDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(msgMigrationService.detail(id));
    }

    @PutMapping("/samples/{id}")
    public ApiResponse<MsgSampleDTO> update(@PathVariable Long id,
                                            @RequestBody MsgSampleUpdateRequest request) {
        return ApiResponse.ok(msgMigrationService.update(id, request));
    }
}
