package cn.sunline.lvy.service;

import java.util.List;

/**
 * bxds 报文源读取接口；抽象出来便于对采集逻辑做单元测试。
 */
public interface MsgSourceReader {

    /** 返回按日期从近到远排序的日分区名（pYYYYMMDD）。 */
    List<String> listPartitionsDesc();

    /**
     * 查询指定分区内满足服务码与单一报文类型（bzjson / soap）的记录，最多返回 limit 条。
     */
    List<MsgSourceRecord> queryPartition(String partitionName, String serviceCode, String messageType, int limit);
}
