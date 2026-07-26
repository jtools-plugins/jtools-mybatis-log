package com.lhstack.jtools.mybatis;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包装真实 Executor,在委托执行前后回填参数并打印完整 SQL。
 * <p>
 * 日志走 MyBatis 自带的 {@link LogFactory}: agent 增强的是 MyBatis 自身的类,
 * 该门面必然可用,不引入 slf4j 等外部日志依赖,避免宿主缺少依赖时应用无法启动。
 */
public class ExecutorWrapper implements Executor {

    private static final Log LOGGER = LogFactory.getLog(ExecutorWrapper.class);

    private static final String ANSI_RESET = "\u001B[0m";

    private final Executor executor;
    private final Configuration configuration;
    private final String sqlFormatType;
    private final String ansiCode;
    private final boolean sqlFormatEnable;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> excludePackages;

    public ExecutorWrapper(Configuration configuration, Executor result, String sqlFormatType, String ansiCode, String excludePackages, boolean sqlFormatEnable) {
        this.executor = result;
        this.configuration = configuration;
        this.sqlFormatType = sqlFormatType;
        this.ansiCode = ansiCode;
        this.sqlFormatEnable = sqlFormatEnable;
        this.excludePackages = parseExcludePackages(excludePackages);
    }

    private static List<String> parseExcludePackages(String excludePackages) {
        if (excludePackages == null || excludePackages.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> patterns = new ArrayList<String>();
        for (String pattern : excludePackages.split(",")) {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }
        return Collections.unmodifiableList(patterns);
    }

    // region SQL 生成

    public String genSql(MappedStatement statement, BoundSql boundSql, Object parameter) {
        try {
            String sql = boundSql.getSql();
            int[] placeholders = PlaceholderScanner.positions(sql);
            String filledSql = fillParameters(statement, boundSql, parameter, sql, placeholders);
            String pagedSql = PaginationAppender.append(filledSql, parameter);
            return sqlFormatEnable ? SqlFormatSupport.format(pagedSql, sqlFormatType) : compressSql(pagedSql);
        } catch (Throwable e) {
            LOGGER.error("[jtools-mybatis-log] gen sql failure, statement id: " + safeStatementId(statement), e);
            return "";
        }
    }

    /**
     * 把参数值回填到占位符位置。字符串字面量与注释中的 {@code ?} 不算占位符,
     * 否则会导致后续所有参数错位。
     */
    private String fillParameters(MappedStatement statement, BoundSql boundSql, Object parameter,
                                  String sql, int[] placeholders) throws SQLException {
        if (placeholders.length == 0) {
            return sql;
        }
        ParameterHandler parameterHandler = configuration.newParameterHandler(statement, parameter, boundSql);
        MockPreparedStatement mockPreparedStatement = new MockPreparedStatement(placeholders.length);
        parameterHandler.setParameters(mockPreparedStatement);
        List<String> values = mockPreparedStatement.getParameters();

        StringBuilder sb = new StringBuilder(sql.length() + placeholders.length * 8);
        int copiedUpTo = 0;
        for (int i = 0; i < placeholders.length; i++) {
            int position = placeholders[i];
            sb.append(sql, copiedUpTo, position);
            String value = values.get(i + 1);
            // 未被捕获的参数保留原始占位符,不伪装成 null
            sb.append(value == null ? "?" : value);
            copiedUpTo = position + 1;
        }
        sb.append(sql, copiedUpTo, sql.length());
        return sb.toString();
    }

    private String compressSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    private String safeStatementId(MappedStatement statement) {
        try {
            return statement.getId();
        } catch (Throwable e) {
            return "<unknown>";
        }
    }

    // endregion

    // region 日志输出

    private boolean isExcluded(String statementId) {
        for (String excludePackage : excludePackages) {
            if (matcher.match(excludePackage, statementId)) {
                return true;
            }
        }
        return false;
    }

    private void log(String id, String sql, long time) {
        LOGGER.warn(id + "\r\n\u001B[" + ansiCode + "m" + sql + ANSI_RESET + "\r\n执行耗时: " + time + "ms");
    }

    // endregion

    // region Executor 委托

    /**
     * 嵌套包装时只透传: 外层已经打印过同一条语句。
     */
    private boolean shouldSkipLogging(MappedStatement ms) {
        return executor instanceof ExecutorWrapper || isExcluded(ms.getId());
    }

    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        if (shouldSkipLogging(ms)) {
            return executor.update(ms, parameter);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.update(ms, parameter);
        } finally {
            log(ms.getId(), sql, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {
        if (shouldSkipLogging(ms)) {
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }
        String sql = genSql(ms, boundSql, parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        } finally {
            log(ms.getId(), sql, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        if (shouldSkipLogging(ms)) {
            return executor.query(ms, parameter, rowBounds, resultHandler);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.query(ms, parameter, rowBounds, resultHandler);
        } finally {
            log(ms.getId(), sql, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
        if (shouldSkipLogging(ms)) {
            return executor.queryCursor(ms, parameter, rowBounds);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.queryCursor(ms, parameter, rowBounds);
        } finally {
            log(ms.getId(), sql, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public List<BatchResult> flushStatements() throws SQLException {
        return executor.flushStatements();
    }

    @Override
    public void commit(boolean required) throws SQLException {
        executor.commit(required);
    }

    @Override
    public void rollback(boolean required) throws SQLException {
        executor.rollback(required);
    }

    @Override
    public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        return executor.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    }

    @Override
    public boolean isCached(MappedStatement ms, CacheKey key) {
        return executor.isCached(ms, key);
    }

    @Override
    public void clearLocalCache() {
        executor.clearLocalCache();
    }

    @Override
    public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
        executor.deferLoad(ms, resultObject, property, key, targetType);
    }

    @Override
    public Transaction getTransaction() {
        return executor.getTransaction();
    }

    @Override
    public void close(boolean forceRollback) {
        executor.close(forceRollback);
    }

    @Override
    public boolean isClosed() {
        return executor.isClosed();
    }

    @Override
    public void setExecutorWrapper(Executor executor) {
        this.executor.setExecutorWrapper(executor);
    }

    // endregion
}
