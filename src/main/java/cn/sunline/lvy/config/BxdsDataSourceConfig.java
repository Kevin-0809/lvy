package cn.sunline.lvy.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据源配置。
 *
 * <p>本地 H2（{@code spring.datasource}）作为主数据源，存报文样本；
 * bxds（{@code ivy.datasource.bxds}）作为只读源数据源，用于从
 * {@code msg_flow_log_request} 采集报文。
 *
 * <p>由于存在多个 DataSource，必须显式声明主数据源并标记 {@link Primary}，
 * 否则 Spring Boot 的数据源自动配置会因已存在 DataSource 而不再创建主数据源。
 */
@Configuration
public class BxdsDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("ivy.datasource.tss")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("ivy.datasource.tss.hikari")
    public HikariDataSource dataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("ivy.datasource.bxds")
    public DataSourceProperties bxdsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("ivy.datasource.bxds.hikari")
    public HikariDataSource bxdsDataSource(@Qualifier("bxdsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public NamedParameterJdbcTemplate bxdsJdbcTemplate(@Qualifier("bxdsDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
