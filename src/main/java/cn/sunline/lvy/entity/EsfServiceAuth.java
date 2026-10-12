package cn.sunline.lvy.entity;

import java.time.LocalDateTime;

/**
 * ESF 服务验证信息，来自原 tss.tss_service_auth，迁移到本地主库。
 */
public class EsfServiceAuth {

    private String protocolId;

    private String sid;

    private String gk;

    private String wk;

    private String pk;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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

    public String getGk() {
        return gk;
    }

    public void setGk(String gk) {
        this.gk = gk;
    }

    public String getWk() {
        return wk;
    }

    public void setWk(String wk) {
        this.wk = wk;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
