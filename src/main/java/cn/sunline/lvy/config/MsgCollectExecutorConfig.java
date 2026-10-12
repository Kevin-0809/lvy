package cn.sunline.lvy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 报文采集线程池：并发度有界，队列承接超额任务，避免一次性提交多个服务码时拖垮 bxds 源库。
 */
@Configuration
public class MsgCollectExecutorConfig {

    @Bean(name = "msgCollectExecutor")
    public ThreadPoolTaskExecutor msgCollectExecutor(
            @Value("${ivy.msg-migration.concurrency:4}") int concurrency,
            @Value("${ivy.msg-migration.queue-capacity:1000}") int queueCapacity) {
        int poolSize = Math.max(1, concurrency);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(poolSize, queueCapacity));
        executor.setThreadNamePrefix("msg-collect-");
        executor.initialize();
        return executor;
    }
}
