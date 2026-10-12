package cn.sunline.lvy.service;

import cn.sunline.lvy.dto.MsgCollectRequest;
import cn.sunline.lvy.dto.MsgCollectResult;
import cn.sunline.lvy.entity.MsgSample;
import cn.sunline.lvy.repository.MsgSampleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MsgMigrationServiceTest {

    private static final Charset GBK = Charset.forName("GBK");

    @Test
    void collectsEachMessageTypeUpToLimit() {
        MsgSourceReader reader = new StubReader(
                List.of("p20260910", "p20260909", "p20260908"),
                Map.of(
                        "p20260910", List.of(record("T1", "bzjson"), record("T2", "soap")),
                        "p20260909", List.of(record("T3", "bzjson"), record("T4", "soap"))));
        MsgSampleRepository repository = mock(MsgSampleRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MsgMigrationService service = new MsgMigrationService(reader, repository, "UTF-8");

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCode("S001");
        request.setLimit(2);
        MsgCollectResult result = service.collect(request);

        assertThat(result.getCollected()).isEqualTo(4);
        assertThat(result.getCollectedByType()).containsEntry("bzjson", 2).containsEntry("soap", 2);
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getScannedPartitions()).isEqualTo(2);
        assertThat(result.getHitPartitions()).containsExactly("p20260910", "p20260909");
    }

    @Test
    void completesWhenEachTypeReachesLimitEvenIfOneTypeMissing() {
        MsgSourceReader reader = new StubReader(
                List.of("p20260910"),
                Map.of("p20260910", List.of(
                        record("T1", "bzjson"), record("T2", "bzjson"), record("T3", "soap"))));
        MsgSampleRepository repository = mock(MsgSampleRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MsgMigrationService service = new MsgMigrationService(reader, repository, "UTF-8");

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCode("S001");
        request.setLimit(2);
        MsgCollectResult result = service.collect(request);

        assertThat(result.getCollectedByType()).containsEntry("bzjson", 2).containsEntry("soap", 1);
        assertThat(result.getCollected()).isEqualTo(3);
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void skipsDuplicatesAndDoesNotCountThem() {
        MsgSourceReader reader = new StubReader(
                List.of("p20260910"),
                Map.of("p20260910", List.of(record("T1", "bzjson"), record("T2", "soap"))));
        MsgSampleRepository repository = mock(MsgSampleRepository.class);
        when(repository.existsByTransIdAndSourceIpAndMessageType(anyString(), anyString(), anyString()))
                .thenReturn(true);
        MsgMigrationService service = new MsgMigrationService(reader, repository, "UTF-8");

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCode("S001");
        request.setLimit(5);
        MsgCollectResult result = service.collect(request);

        assertThat(result.getCollected()).isZero();
        assertThat(result.getSkipped()).isEqualTo(2);
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void decodesGbkWhenNotValidUtf8() {
        byte[] gbkContent = "交易成功".getBytes(GBK);
        MsgSourceReader reader = new StubReader(
                List.of("p20260910"),
                Map.of("p20260910", List.of(
                        new MsgSourceRecord("T1", "10.0.0.1", "bzjson", 1L, "G1", "8001", gbkContent))));
        MsgSampleRepository repository = mock(MsgSampleRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MsgMigrationService service = new MsgMigrationService(reader, repository, "UTF-8");

        MsgCollectRequest request = new MsgCollectRequest();
        request.setServiceCode("S001");
        request.setLimit(1);
        service.collect(request);

        ArgumentCaptor<MsgSample> captor = ArgumentCaptor.forClass(MsgSample.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("交易成功");
    }

    private static MsgSourceRecord record(String transId, String messageType) {
        return new MsgSourceRecord(transId, "10.0.0.1", messageType, 1L, "G" + transId, "8001",
                ("<" + transId + "/>").getBytes(StandardCharsets.UTF_8));
    }

    private static final class StubReader implements MsgSourceReader {

        private final List<String> partitions;
        private final Map<String, List<MsgSourceRecord>> data;

        private StubReader(List<String> partitions, Map<String, List<MsgSourceRecord>> data) {
            this.partitions = partitions;
            this.data = data;
        }

        @Override
        public List<String> listPartitionsDesc() {
            return partitions;
        }

        @Override
        public List<MsgSourceRecord> queryPartition(String partitionName, String serviceCode,
                                                    String messageType, int limit) {
            return data.getOrDefault(partitionName, List.of()).stream()
                    .filter(record -> messageType.equalsIgnoreCase(record.messageType()))
                    .limit(limit)
                    .toList();
        }
    }
}
