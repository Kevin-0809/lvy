package cn.sunline.lvy.entity;

import java.time.LocalDateTime;

/**
 * 本地报文样本：从 bxds.msg_flow_log_request 采集的明文报文及其元数据。
 */
public class MsgSample {

    private Long id;

    private String serviceCode;

    private String partitionName;

    private String transId;

    private String sourceIp;

    private Long txnTime;

    private String messageType;

    private String globalSeqNo;

    private String tranTellerNo;

    private String content;

    private boolean edited = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public Long getTxnTime() {
        return txnTime;
    }

    public void setTxnTime(Long txnTime) {
        this.txnTime = txnTime;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getGlobalSeqNo() {
        return globalSeqNo;
    }

    public void setGlobalSeqNo(String globalSeqNo) {
        this.globalSeqNo = globalSeqNo;
    }

    public String getTranTellerNo() {
        return tranTellerNo;
    }

    public void setTranTellerNo(String tranTellerNo) {
        this.tranTellerNo = tranTellerNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
