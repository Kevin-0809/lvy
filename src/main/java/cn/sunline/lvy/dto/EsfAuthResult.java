package cn.sunline.lvy.dto;

/**
 * ESF 验证信息结果。
 */
public class EsfAuthResult {

    private String protocolId;
    private String sid;
    private String authContent;

    public EsfAuthResult() {
    }

    public EsfAuthResult(String protocolId, String sid, String authContent) {
        this.protocolId = protocolId;
        this.sid = sid;
        this.authContent = authContent;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getAuthContent() {
        return authContent;
    }

    public void setAuthContent(String authContent) {
        this.authContent = authContent;
    }
}
