package cn.sunline.lvy.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.spi.Limit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class GaussPaginationTest {
    @Test
    void configuredDialectUsesLimitOffsetForBothFirstAndLaterPages() throws Exception {
        var source = new YamlPropertySourceLoader().load("application",
                new ClassPathResource("application.yml")).get(0);
        String className = (String) source.getProperty("spring.jpa.properties.hibernate.dialect");
        Dialect dialect = (Dialect) Class.forName(className).getDeclaredConstructor().newInstance();
        assertThat(dialect.supportsFetchClause(org.hibernate.query.sqm.FetchClauseType.ROWS_ONLY)).isFalse();
        for (int offset : new int[]{0, 20}) {
            Limit limit = new Limit();
            limit.setFirstRow(offset);
            limit.setMaxRows(20);
            String sql = dialect.getLimitHandler().processSql("select id from ivy_msg_sample order by id", limit);
            assertThat(sql).contains("limit ?").doesNotContain("fetch first");
            if (offset > 0) assertThat(sql).contains("offset ?");
        }
        assertThat(((org.hibernate.dialect.pagination.AbstractLimitHandler) dialect.getLimitHandler())
                .bindLimitParametersInReverseOrder()).isTrue();
    }
}
