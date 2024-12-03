package com.jtools.mybatislog;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ExecutorWrapper implements Executor {
    private final Executor executor;
    private final Configuration configuration;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorWrapper.class);

    public ExecutorWrapper(Configuration configuration, Executor result) {
        this.executor = result;
        this.configuration = configuration;
    }


    public void showSql(MappedStatement statement, BoundSql boundSql, Object parameter) {
        try {
            String sql = boundSql.getSql();
            ParameterHandler parameterHandler = configuration.newParameterHandler(statement, parameter, boundSql);
            MockPreparedStatement mockPreparedStatement = null;
            Page<?> page = null;
            if (parameter instanceof Map) {
                Map<?, ?> param = (Map<?, ?>) parameter;
                if (param.containsKey("page")) {
                    Object p = param.get("page");
                    if (p instanceof Page) {
                        page = (Page<?>) p;
                    }
                }
                int idx = 0;
                char[] charArray = sql.toCharArray();
                for (char c : charArray) {
                    if(c == '?'){
                        idx ++;
                    }
                }
                mockPreparedStatement = new MockPreparedStatement(idx);
                parameterHandler.setParameters(mockPreparedStatement);
            }
            StringBuilder sb = new StringBuilder();
            char[] charArray = sql.toCharArray();
            int idx = 1;
            for (char c : charArray) {
                if (c == '?') {
                    sb.append(mockPreparedStatement.getParameters().get(idx++));
                } else {
                    sb.append(c);
                }
            }
            if (page != null) {
                sb.append(" LIMIT ");
                if (page.getCurrent() <= 1) {
                    sb.append(page.getSize());
                } else {
                    sb.append(page.getCurrent()).append(" , ").append(page.getSize());
                }
            }
            LOGGER.error("{}\r\n{}\r\n", statement.getId(), SqlFormatter.format(sb.toString()));
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        showSql(ms, ms.getBoundSql(parameter), parameter);
        return executor.update(ms, parameter);
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {
        showSql(ms, boundSql, parameter);
        return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        showSql(ms, ms.getBoundSql(parameter), parameter);
        return executor.query(ms, parameter, rowBounds, resultHandler);
    }

    @Override
    public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
        return executor.queryCursor(ms, parameter, rowBounds);
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
        executor.setExecutorWrapper(executor);
    }
}
