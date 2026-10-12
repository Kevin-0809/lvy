package cn.sunline.lvy.config;

import cn.sunline.lvy.entity.EsfServiceAuth;
import cn.sunline.lvy.repository.EsfServiceAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化本地 tss_service_auth 数据；本地表为空时按原始样例写入。
 */
@Component
public class EsfAuthDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EsfAuthDataSeeder.class);
    private static final String SID = "10530013";
    private static final String GK = "P3FWPOZ4TmU8TV4jOiFDJA==";
    private static final String WK_528 = "8ODkUKS5SnKloea3uh7KyQ==";
    private static final String PK_528 = "he4KpKWTQbYHINRahOZ+Fw==";
    private static final String WK_CCBS = "IEd88Dq79TeRb9Xs0+z6rg==";
    private static final String PK_CCBS = "5UzcYp4WeGLFZE6opPyhw==";

    private final EsfServiceAuthRepository repository;

    public EsfAuthDataSeeder(EsfServiceAuthRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<EsfServiceAuth> rows = List.of(
                auth("528_json", WK_528, PK_528),
                auth("528_xml", WK_528, PK_528),
                auth("528_sop", WK_528, PK_528),
                auth("528_spec", WK_528, PK_528),
                auth("ccbs_json", WK_CCBS, PK_CCBS),
                auth("ccbs_xml", WK_CCBS, PK_CCBS),
                auth("ccbs_sop", WK_CCBS, PK_CCBS),
                auth("ccbs_spec", WK_CCBS, PK_CCBS));
        repository.saveAll(rows);
        log.info("本地 tss_service_auth 初始化完成，共 {} 条", rows.size());
    }

    private EsfServiceAuth auth(String protocolId, String wk, String pk) {
        EsfServiceAuth auth = new EsfServiceAuth();
        auth.setProtocolId(protocolId);
        auth.setSid(SID);
        auth.setGk(GK);
        auth.setWk(wk);
        auth.setPk(pk);
        return auth;
    }
}
