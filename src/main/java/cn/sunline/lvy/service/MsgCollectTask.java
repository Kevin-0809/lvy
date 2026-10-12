package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.MsgCollectResult;
import cn.sunline.lvy.dto.MsgCollectTaskDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集任务运行时状态：按服务码记录进度，供轮询读取。
 *
 * <p>内部状态只在采集线程与轮询线程间共享，所有读写方法均在对象锁内完成并返回副本快照。
 */
public class MsgCollectTask {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private final String taskId;
    private final int limit;
    private final List<String> serviceCodes;
    private final Map<String, MsgCollectResult> results = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();

    private String status = STATUS_PENDING;
    private final long submittedAt;
    private long startedAt;
    private long finishedAt;
    private int finished;
    private int remaining;

    public MsgCollectTask(String taskId, List<String> serviceCodes, int limit) {
        this.taskId = taskId;
        this.limit = limit;
        this.serviceCodes = new ArrayList<>(serviceCodes);
        this.remaining = serviceCodes.size();
        this.submittedAt = System.currentTimeMillis();
        for (String code : serviceCodes) {
            results.put(code, emptyResult(code, limit));
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public List<String> getServiceCodes() {
        return new ArrayList<>(serviceCodes);
    }

    public synchronized void start() {
        this.status = STATUS_RUNNING;
        this.startedAt = System.currentTimeMillis();
    }

    /** 汇报某个服务码的最新采集进度快照。 */
    public synchronized void report(String serviceCode, MsgCollectResult result) {
        results.put(serviceCode, result.copy());
    }

    /** 记录某个服务码采集失败，保留已有进度。 */
    public synchronized void fail(String serviceCode, String message) {
        errors.put(serviceCode, message == null ? "采集失败" : message);
        MsgCollectResult current = results.get(serviceCode);
        if (current != null) {
            current.setCompleted(false);
        }
    }

    /** 单个服务码处理结束（无论成功或失败）。 */
    public synchronized void finishOne() {
        finished++;
        remaining = Math.max(remaining - 1, 0);
        if (remaining == 0) {
            finishedAt = System.currentTimeMillis();
            status = finished > 0 && errors.size() == finished ? STATUS_FAILED : STATUS_COMPLETED;
        }
    }

    public synchronized MsgCollectTaskDTO snapshot() {
        MsgCollectTaskDTO dto = new MsgCollectTaskDTO();
        dto.setTaskId(taskId);
        dto.setStatus(status);
        dto.setTotal(serviceCodes.size());
        dto.setFinished(finished);
        dto.setFailed(errors.size());
        dto.setRequested(limit);
        dto.setSubmittedAt(submittedAt);
        dto.setStartedAt(startedAt);
        dto.setFinishedAt(finishedAt);

        int collected = 0;
        int skipped = 0;
        List<MsgCollectResult> snapshotResults = new ArrayList<>();
        for (String code : serviceCodes) {
            MsgCollectResult result = results.get(code);
            if (result == null) {
                result = emptyResult(code, limit);
            }
            collected += result.getCollected();
            skipped += result.getSkipped();
            snapshotResults.add(result.copy());
        }
        dto.setCollected(collected);
        dto.setSkipped(skipped);
        dto.setResults(snapshotResults);
        dto.setErrors(new LinkedHashMap<>(errors));
        return dto;
    }

    private static MsgCollectResult emptyResult(String serviceCode, int limit) {
        MsgCollectResult result = new MsgCollectResult();
        result.setServiceCode(serviceCode);
        result.setRequested(limit);
        result.getCollectedByType().put("bzjson", 0);
        result.getCollectedByType().put("soap", 0);
        return result;
    }
}
