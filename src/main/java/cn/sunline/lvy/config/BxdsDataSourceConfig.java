package cn.sunline.lvy.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置。
 *
 * <p>tss（{@code ivy.datasource.tss}）为主数据源，存报文样本、集合、历史等本地数据；
 * bxds（{@code ivy.datasource.bxds}）为只读源数据源，用于从
 * {@code msg_flow_log_request} 采集报文。
 *
 * <p>由于存在多个 DataSource，必须显式声明主数据源、主库 JdbcTemplate 与事务管理器，
 * 否则 Spring Boot 的数据源/JdbcTemplate 自动配置会因存在多个候选而退避。
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

    /** 主库 JdbcTemplate，供各 DAO 直接使用。 */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /** 主库事务管理器，配合 {@code @Transactional} 使用。 */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate bxdsJdbcTemplate(@Qualifier("bxdsDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
