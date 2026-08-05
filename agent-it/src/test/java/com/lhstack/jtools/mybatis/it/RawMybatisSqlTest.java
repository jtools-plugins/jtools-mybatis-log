package com.lhstack.jtools.mybatis.it;

import com.lhstack.jtools.mybatis.it.mapper.ExcludedMapper;
import com.lhstack.jtools.mybatis.it.mapper.RawUserMapper;
import com.lhstack.jtools.mybatis.it.support.ItBase;
import com.lhstack.jtools.mybatis.it.support.LogRecords;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯 MyBatis 路径: 占位符扫描、字面量渲染与排除规则。
 */
class RawMybatisSqlTest extends ItBase {

    @Test
    @DisplayName("参数被回填成字面量")
    void fillsParameterAsLiteral() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).findByName("alice");
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByName");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(sql.contains("name = 'alice'"), "参数未回填: " + sql);
        assertTrue(!sql.contains("?"), "仍残留占位符: " + sql);
    }

    @Test
    @DisplayName("参数值里的单引号被转义成 ''")
    void escapesSingleQuote() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).findByName("o'brien");
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByName");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(sql.contains("'o''brien'"), "单引号未转义: " + sql);
    }

    @Test
    @DisplayName("字符串字面量内的问号不参与参数计数")
    void ignoresQuestionMarkInLiteral() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).findByLiteralQuestion(10);
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByLiteralQuestion");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        // 'a?b' 必须原样保留, age 的值要落在正确位置
        assertTrue(sql.contains("remark = 'a?b'"), "字面量被破坏: " + sql);
        assertTrue(sql.contains("age > 10"), "参数错位: " + sql);
    }

    @Test
    @DisplayName("注释内的问号不参与参数计数")
    void ignoresQuestionMarkInComments() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).findWithComments("bob", 10);
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findWithComments");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(sql.contains("name = 'bob'"), "第一个参数错位: " + sql);
        assertTrue(sql.contains("age > 10"), "第二个参数错位: " + sql);
    }

    @Test
    @DisplayName("同一参数被多次引用时都能回填")
    void fillsRepeatedParameter() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).countByTime(null);
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("countByTime");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(!sql.contains("?"), "仍残留占位符: " + sql);
    }

    @Test
    @DisplayName("排除的 mapper 不打印 SQL, 且模式两侧空格被 trim")
    void excludedMapperPrintsNothing() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(ExcludedMapper.class).countAll();
            session.getMapper(RawUserMapper.class).findByName("alice");
        }
        assertNoAgentError();

        assertEquals(null, LogRecords.lastSqlOf("countAll"),
                "被排除的 mapper 仍打印了 SQL:\n" + LogRecords.dump());
        assertNotNull(LogRecords.lastSqlOf("findByName"), "未排除的 mapper 应正常打印");
    }
}
