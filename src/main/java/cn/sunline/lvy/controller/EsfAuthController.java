package cn.sunline.lvy.controller;

import cn.sunline.lvy.dto.ApiResponse;
import cn.sunline.lvy.dto.EsfAuthRequest;
import cn.sunline.lvy.dto.EsfAuthResult;
import cn.sunline.lvy.service.EsfAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ESF 验证信息接口。
 */
@RestController
@RequestMapping("/api/esf-auth")
public class EsfAuthController {

    private final EsfAuthService esfAuthService;

    public EsfAuthController(EsfAuthService esfAuthService) {
        this.esfAuthService = esfAuthService;
    }

    @PostMapping("/generate")
    public ApiResponse<EsfAuthResult> generate(@RequestBody EsfAuthRequest request) {
        return ApiResponse.ok(esfAuthService.generate(request));
    }
}
