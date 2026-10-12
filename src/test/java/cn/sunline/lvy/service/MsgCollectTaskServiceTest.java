package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.MsgCollectRequest;
import cn.sunline.lvy.dto.MsgCollectResult;
import cn.sunline.lvy.dto.MsgCollectTaskDTO;
import cn.sunline.lvy.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MsgCollectTaskServiceTest {

    @Test
    void aggregatesMultipleServiceCodesAndDeduplicates() {
        MsgCollectTaskService service = new MsgCollectTaskService(stubMigration(), Runnable::run);

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCodes(List.of("S001, S002", "S003", "S001"));
        request.setLimit(1);
        MsgCollectTaskDTO task = service.submit(request);

        assertThat(task.getTotal()).isEqualTo(3);
        assertThat(task.getFinished()).isEqualTo(3);
        assertThat(task.getStatus()).isEqualTo(MsgCollectTask.STATUS_COMPLETED);
        assertThat(task.getCollected()).isEqualTo(6);
        assertThat(task.getSkipped()).isZero();
        assertThat(task.getResults())
                .extracting(MsgCollectResult::getServiceCode)
                .containsExactly("S001", "S002", "S003");
    }

    @Test
    void marksFailedWhenAllServicesFail() {
        MsgMigrationService migration = mock(MsgMigrationService.class);
        when(migration.collectOne(anyString(), anyInt(), any())).thenThrow(new RuntimeException("boom"));
        MsgCollectTaskService service = new MsgCollectTaskService(migration, Runnable::run);

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCodes(List.of("S001"));
        request.setLimit(1);
        MsgCollectTaskDTO task = service.submit(request);

        assertThat(task.getStatus()).isEqualTo(MsgCollectTask.STATUS_FAILED);
        assertThat(task.getFailed()).isEqualTo(1);
        assertThat(task.getErrors()).containsKey("S001");
    }

    @Test
    void rejectsEmptyServiceCodes() {
        MsgCollectTaskService service = new MsgCollectTaskService(stubMigration(), Runnable::run);

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCodes(List.of("  ", ",", ""));

        assertThatThrownBy(() -> service.submit(request)).isInstanceOf(BizException.class);
    }

    @Test
    void statusThrowsWhenTaskMissing() {
        MsgCollectTaskService service = new MsgCollectTaskService(stubMigration(), Runnable::run);
        assertThatThrownBy(() -> service.status("not-exist")).isInstanceOf(BizException.class);
    }

    private static MsgMigrationService stubMigration() {
        MsgMigrationService migration = mock(MsgMigrationService.class);
        when(migration.collectOne(anyString(), anyInt(), any())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            Consumer<MsgCollectResult> progress = invocation.getArgument(2);
            MsgCollectResult result = new MsgCollectResult();
            result.setServiceCode(code);
            result.setCollected(2);
            result.setCompleted(true);
            result.getCollectedByType().put("bzjson", 1);
            result.getCollectedByType().put("soap", 1);
            if (progress != null) {
                progress.accept(result);
            }
            return result;
        });
        return migration;
    }
}
