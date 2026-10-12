package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.EsfAuthRequest;
import cn.sunline.lvy.dto.EsfAuthResult;
import cn.sunline.lvy.entity.EsfServiceAuth;
import cn.sunline.lvy.exception.BizException;
import cn.sunline.lvy.repository.EsfServiceAuthRepository;
import cn.sunline.lvy.util.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * ESF 验证信息：按环境 + 报文类型拼出 protocol_id，从本地主库 tss_service_auth 取
 * sid/gk/pk/wk，并调用 {@link AuthUtil#packToken} 生成 authContent。
 */
@Service
public class EsfAuthService {

    private final EsfServiceAuthRepository repository;

    public EsfAuthService(EsfServiceAuthRepository repository) {
        this.repository = repository;
    }

    public EsfAuthResult generate(EsfAuthRequest request) {
        String environmentId = request.getEnvironmentId() == null ? "" : request.getEnvironmentId().trim();
        if (environmentId.isEmpty()) {
            throw new BizException(400, "环境 ID 不能为空");
        }
        String messageType = request.getMessageType() == null ? "" : request.getMessageType().trim();
        if (messageType.isEmpty()) {
            throw new BizException(400, "报文类型不能为空");
        }

        String protocolId = environmentId.toLowerCase(Locale.ROOT) + "_" + toProtocolType(messageType);
        EsfServiceAuth auth = repository.findById(protocolId)
                .orElseThrow(() -> new BizException(404, "未找到 protocol_id=" + protocolId + " 的验证信息"));
        String authContent;
        try {
            authContent = AuthUtil.packToken(auth.getSid(), auth.getGk(), auth.getPk(), auth.getWk());
        } catch (Exception e) {
            throw new BizException(500, "生成 authContent 失败: " + e.getMessage());
        }
        return new EsfAuthResult(protocolId, auth.getSid(), authContent);
    }

    private String toProtocolType(String messageType) {
        String type = messageType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "bzjson" -> "json";
            case "soap" -> "xml";
            case "sop" -> "sop";
            case "sop2cbsp" -> "spec";
            default -> type;
        };
    }
}
