package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.ProxyRequest;
import cn.sunline.lvy.dto.ProxyResponse;
import cn.sunline.lvy.service.ProxyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理发送接口。
 */
@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/send")
    public ApiResponse<ProxyResponse> send(@Valid @RequestBody ProxyRequest request) {
        return ApiResponse.ok(proxyService.send(request));
    }
}
