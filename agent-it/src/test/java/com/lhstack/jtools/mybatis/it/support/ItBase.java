package com.lhstack.jtools.mybatis.it.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.lhstack.jtools.mybatis.it.mapper.ExcludedMapper;
import com.lhstack.jtools.mybatis.it.mapper.RawUserMapper;
import com.lhstack.jtools.mybatis.it.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 集成测试基类: 构建被 agent 增强过的 SqlSessionFactory。
 * <p>
 * 关键顺序: 必须先注册 CapturingLog,再触发 ExecutorWrapper 的类初始化,
 * 否则它的静态 LOGGER 会绑定到默认实现,断言拿不到日志。
 */
public abstract class ItBase {

    private static final AtomicInteger DB_SEQ = new AtomicInteger();

    protected SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void useCapturingLog() {
        LogFactory.useCustomLogging(CapturingLog.class);
    }

    @BeforeEach
    void setUpFactory() throws Exception {
        LogRecords.clear();
        this.sqlSessionFactory = buildFactory(newDataSource());
    }

    /**
     * 每个测试方法一个独立内存库,避免用例间互相影响。
     */
    private DataSource newDataSource() throws Exception {
        String url = "jdbc:h2:mem:jtools_it_" + DB_SEQ.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
        DataSource dataSource = new UnpooledDataSource("org.h2.Driver", url, "sa", "");
        try (Connection connection = dataSource.getConnection();
             Reader reader = new InputStreamReader(Resources.getResourceAsStream("schema.sql"), "UTF-8")) {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runner.runScript(reader);
        }
        return dataSource;
    }

    /**
     * 用 MybatisSqlSessionFactoryBuilder + MybatisConfiguration:
     * agent 增强的正是 MybatisConfiguration.newExecutor。
     */
    private SqlSessionFactory buildFactory(DataSource dataSource) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("it", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(UserMapper.class);
        configuration.addMapper(RawUserMapper.class);
        configuration.addMapper(ExcludedMapper.class);
        registerInterceptors(configuration);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    /** 子类按需装分页插件 */
    protected void registerInterceptors(MybatisConfiguration configuration) {
    }

    /**
     * 每个用例都断言 agent 自身没有报错。
     * gen sql failure 会被 agent 吞成空 SQL,不看错误日志就发现不了。
     */
    protected void assertNoAgentError() {
        List<String> errors = LogRecords.agentErrors();
        if (!errors.isEmpty()) {
            throw new AssertionError("agent 输出了错误日志:\n" + String.join("\n", errors)
                    + "\n完整日志:\n" + LogRecords.dump());
        }
    }
}
