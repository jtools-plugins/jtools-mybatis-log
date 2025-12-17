package com.jtools.mybatislog;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
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

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutorWrapper implements Executor {
    private final Executor executor;
    private final Configuration configuration;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorWrapper.class);
    private final String sqlFormatType;
    private final String ansiCode;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private List<String> excludePackages = Collections.emptyList();

    public ExecutorWrapper(Configuration configuration, Executor result, String sqlFormatType, String ansiCode, String excludePackages) {
        this.executor = result;
        this.configuration = configuration;
        this.sqlFormatType = sqlFormatType;
        this.ansiCode = ansiCode;
        if(excludePackages != null && !excludePackages.isEmpty()) {
            this.excludePackages = Arrays.asList(excludePackages.split(","));
        }
    }


    public String genSql(MappedStatement statement, BoundSql boundSql, Object parameter) {
        try {
            String sql = boundSql.getSql();
            ParameterHandler parameterHandler = configuration.newParameterHandler(statement, parameter, boundSql);
            MockPreparedStatement mockPreparedStatement = null;
            StringBuilder sb = new StringBuilder();
            char[] charArray = sql.toCharArray();
            int idx = 0;
            for (char c : charArray) {
                if (c == '?') {
                    idx++;
                }
            }
            mockPreparedStatement = new MockPreparedStatement(idx);
            parameterHandler.setParameters(mockPreparedStatement);
            idx = 1;
            for (char c : charArray) {
                if (c == '?') {
                    sb.append(mockPreparedStatement.getParameters().get(idx++));
                } else {
                    sb.append(c);
                }
            }

            try {
                Page<?> page = null;
                if (parameter instanceof Map) {
                    Map<?, ?> param = (Map<?, ?>) parameter;
                    if (param.containsKey("page")) {
                        Object p = param.get("page");
                        if (p instanceof Page) {
                            page = (Page<?>) p;
                        }
                    }
                } else if (parameter instanceof Page) {
                    page = (Page<?>) parameter;
                }
                if (page != null) {
                    sb.append(" LIMIT ");
                    if (page.getCurrent() <= 1) {
                        sb.append(page.getSize());
                    } else {
                        sb.append((page.getCurrent() - 1) * page.getSize()).append(" , ").append(page.getSize());
                    }
                }

            } catch (Throwable ignore) {

            }

            try {
                com.github.pagehelper.Page<Object> localPage = PageHelper.getLocalPage();
                if (localPage != null && localPage.getPageSize() > 0) {
                    sb.append(" LIMIT ");
                    if (localPage.getPageNum() <= 1) {
                        sb.append(localPage.getPageSize());
                    } else {
                        sb.append((localPage.getPageNum() - 1) * localPage.getPageSize()).append(" , ").append(localPage.getPageSize());
                    }
                }
            } catch (Throwable ignore) {

            }
            return SqlFormatter.of(Dialect.nameOf(sqlFormatType).orElse(Dialect.MySql)).format(sb.toString());
        } catch (Throwable e) {
            try {
                LOGGER.error("gen sql failure,statement id: {}", statement.getId(), e);
            } catch (Throwable err) {
                System.out.printf("gen sql failure,statement id: %s,error: %s", statement.getId(), e);
            }
            return "";
        }
    }

    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        if (executor instanceof ExecutorWrapper) {
            return executor.update(ms, parameter);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.update(ms, parameter);
        } finally {
            long time = System.currentTimeMillis() - startTime;
            log(ms.getId(), sql, time);
        }

    }

    private void log(String id, String sql, long time) {
        for (String excludePackage : excludePackages) {
            if (matcher.match(excludePackage, id)) {
                return;
            }
        }
        String code = "\u001B[" + ansiCode + "m";
        try {
            LOGGER.info("{}\r\n{}{}\u001B[0m\r\n执行耗时: {}ms", id, code, sql, time);
        } catch (Throwable e) {
            System.out.printf("%s\r\n%s%s\u001B[0m\r\n执行耗时: %sms", id, code, sql, time);
        }
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {
        if (executor instanceof ExecutorWrapper) {
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        } finally {
            long time = System.currentTimeMillis() - startTime;
            log(ms.getId(), sql, time);
        }
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        if (executor instanceof ExecutorWrapper) {
            return executor.query(ms, parameter, rowBounds, resultHandler);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.query(ms, parameter, rowBounds, resultHandler);
        } finally {
            long time = System.currentTimeMillis() - startTime;
            log(ms.getId(), sql, time);
        }
    }

    @Override
    public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
        if (executor instanceof ExecutorWrapper) {
            return executor.queryCursor(ms, parameter, rowBounds);
        }
        String sql = genSql(ms, ms.getBoundSql(parameter), parameter);
        long startTime = System.currentTimeMillis();
        try {
            return executor.queryCursor(ms, parameter, rowBounds);
        } finally {
            long time = System.currentTimeMillis() - startTime;
            log(ms.getId(), sql, time);
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
}
