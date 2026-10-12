package cn.sunline.lvy.service;

/**
 * 从 bxds.msg_flow_log_request 读取到的一条原始记录；message 为未解码的报文字节。
 */
public record MsgSourceRecord(
        String transId,
        String sourceIp,
        String messageType,
        Long txnTime,
        String globalSeqNo,
        String tranTellerNo,
        byte[] message) {
}
