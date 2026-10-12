package cn.sunline.lvy.service;

import cn.sunline.lvy.entity.MsgSample;
import cn.sunline.lvy.repository.MsgSampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** 显式运行的高斯集成测试：读取已采集数据，不改写数据库。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class GaussMsgReadIT {

    @Autowired
    private MsgMigrationService service;

    @Autowired
    private MsgSampleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void storesLongChineseContentAsTextAndReadsItBack() {
        String content = "{\"说明\":\"" + "中文报文明文😀".repeat(3000) + "\"}";
        MsgSample sample = new MsgSample();
        sample.setServiceCode("IVY_TEXT_ROUNDTRIP_TEST");
        sample.setTransId(java.util.UUID.randomUUID().toString());
        sample.setSourceIp("127.0.0.1");
        sample.setMessageType("bzjson");
        sample.setContent(content);
        repository.save(sample);
        Long id = sample.getId();

        assertThat(service.detail(id).getContent()).isEqualTo(content);
        String stored = jdbcTemplate.queryForObject(
                "select content from ivy_msg_sample where id = ?", String.class, id);
        assertThat(stored).isEqualTo(content);
    }

    @Test
    void readsCollectedContentThroughListAndDetail() {
        var page = service.page(null, 0, 20);
        assertThat(page.getItems()).isNotEmpty();
        var sample = page.getItems().get(0);
        var filtered = service.page(sample.getServiceCode(), 0, 20);
        assertThat(filtered.getItems()).isNotEmpty();
        assertThat(service.detail(sample.getId()).getContent()).isNotEmpty();
    }
}
