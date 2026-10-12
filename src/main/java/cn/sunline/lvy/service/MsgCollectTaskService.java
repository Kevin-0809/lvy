package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.MsgCollectRequest;
import cn.sunline.lvy.dto.MsgCollectResult;
import cn.sunline.lvy.dto.MsgCollectTaskDTO;
import cn.sunline.lvy.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 异步采集任务管理：一次提交多个服务码，交由有界线程池并发采集，前端按任务号轮询进度。
 *
 * <p>任务进度保存在内存中，仅适用于单实例部署；最多保留最近 {@value #MAX_TASKS} 个任务。
 */
@Service
public class MsgCollectTaskService {

    private static final Logger log = LoggerFactory.getLogger(MsgCollectTaskService.class);

    private static final int MAX_TASKS = 50;
    private static final int MAX_CODES = 100;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 1000;

    private final MsgMigrationService migrationService;
    private final Executor executor;
    private final Map<String, MsgCollectTask> tasks = new LinkedHashMap<>();

    public MsgCollectTaskService(MsgMigrationService migrationService,
                                 @Qualifier("msgCollectExecutor") Executor executor) {
        this.migrationService = migrationService;
        this.executor = executor;
    }

    /** 提交采集任务，立即返回任务快照（含任务号）。 */
    public MsgCollectTaskDTO submit(MsgCollectRequest request) {
        List<String> codes = normalize(request.getServiceCodes());
        if (codes.isEmpty()) {
            throw new BizException(400, "服务码不能为空");
        }
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(400, "每类采集数量需在 1-" + MAX_LIMIT + " 之间");
        }

        MsgCollectTask task = new MsgCollectTask(UUID.randomUUID().toString(), codes, limit);
        register(task);
        task.start();
        for (String code : codes) {
            try {
                executor.execute(() -> runService(task, code, limit));
            } catch (RejectedExecutionException e) {
                task.fail(code, "采集线程池繁忙，任务被拒绝");
                task.finishOne();
            }
        }
        return task.snapshot();
    }

    /** 查询任务进度；任务不存在时抛出 404。 */
    public MsgCollectTaskDTO status(String taskId) {
        MsgCollectTask task = find(taskId);
        if (task == null) {
            throw new BizException(404, "采集任务不存在: " + taskId);
        }
        return task.snapshot();
    }

    private void runService(MsgCollectTask task, String code, int limit) {
        try {
            MsgCollectResult result = migrationService.collectOne(code, limit,
                    progress -> task.report(code, progress));
            task.report(code, result);
        } catch (BizException e) {
            task.fail(code, e.getMessage());
        } catch (Exception e) {
            log.warn("服务码 {} 采集失败: {}", code, e.getMessage());
            task.fail(code, "采集失败: " + e.getMessage());
        } finally {
            task.finishOne();
        }
    }

    private void register(MsgCollectTask task) {
        synchronized (tasks) {
            tasks.put(task.getTaskId(), task);
            while (tasks.size() > MAX_TASKS) {
                String oldest = tasks.keySet().iterator().next();
                tasks.remove(oldest);
            }
        }
    }

    private MsgCollectTask find(String taskId) {
        synchronized (tasks) {
            return tasks.get(taskId);
        }
    }

    /** 拆分逗号/换行/空白分隔的输入，去空去重并限制数量。 */
    private List<String> normalize(List<String> raw) {
        Set<String> unique = new LinkedHashSet<>();
        if (raw != null) {
            for (String value : raw) {
                if (value == null) {
                    continue;
                }
                for (String part : value.split("[,，;；\\s]+")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        unique.add(trimmed);
                    }
                }
            }
        }
        List<String> codes = new ArrayList<>(unique);
        if (codes.size() > MAX_CODES) {
            throw new BizException(400, "一次最多采集 " + MAX_CODES + " 个服务码");
        }
        return codes;
    }
}
