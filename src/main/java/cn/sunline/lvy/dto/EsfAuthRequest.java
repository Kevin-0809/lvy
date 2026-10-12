package cn.sunline.lvy.dto;

/**
 * ESF 验证信息生成入参。
 */
public class EsfAuthRequest {

    private String environmentId;
    private String messageType;

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
