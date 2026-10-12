package cn.sunline.lvy.config;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;

/** 高斯兼容分页：使用支持参数绑定的 LIMIT / OFFSET，避免 FETCH FIRST。 */
public class GaussDialect extends PostgreSQLDialect {
    @Override
    public boolean supportsFetchClause(org.hibernate.query.sqm.FetchClauseType type) {
        return false;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return LimitOffsetLimitHandler.INSTANCE;
    }
}
