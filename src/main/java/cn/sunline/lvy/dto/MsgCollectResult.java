package cn.sunline.lvy.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报文采集结果统计。
 */
public class MsgCollectResult {

    private String serviceCode;
    private int requested;
    private int collected;
    private int skipped;
    private int scannedPartitions;
    private boolean completed;
    private List<String> hitPartitions = new ArrayList<>();
    private Map<String, Integer> collectedByType = new LinkedHashMap<>();

    /** 生成快照副本，供异步采集进度轮询读取，避免与写入线程共享可变对象。 */
    public MsgCollectResult copy() {
        MsgCollectResult copy = new MsgCollectResult();
        copy.serviceCode = serviceCode;
        copy.requested = requested;
        copy.collected = collected;
        copy.skipped = skipped;
        copy.scannedPartitions = scannedPartitions;
        copy.completed = completed;
        copy.hitPartitions = new ArrayList<>(hitPartitions);
        copy.collectedByType = new LinkedHashMap<>(collectedByType);
        return copy;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public int getRequested() {
        return requested;
    }

    public void setRequested(int requested) {
        this.requested = requested;
    }

    public int getCollected() {
        return collected;
    }

    public void setCollected(int collected) {
        this.collected = collected;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getScannedPartitions() {
        return scannedPartitions;
    }

    public void setScannedPartitions(int scannedPartitions) {
        this.scannedPartitions = scannedPartitions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<String> getHitPartitions() {
        return hitPartitions;
    }

    public void setHitPartitions(List<String> hitPartitions) {
        this.hitPartitions = hitPartitions;
    }

    public Map<String, Integer> getCollectedByType() {
        return collectedByType;
    }

    public void setCollectedByType(Map<String, Integer> collectedByType) {
        this.collectedByType = collectedByType;
    }
}
