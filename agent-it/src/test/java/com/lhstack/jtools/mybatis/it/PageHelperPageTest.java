package com.lhstack.jtools.mybatis.it;

import com.github.pagehelper.PageHelper;
import com.lhstack.jtools.mybatis.it.mapper.RawUserMapper;
import com.lhstack.jtools.mybatis.it.support.ItBase;
import com.lhstack.jtools.mybatis.it.support.LogRecords;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageHelper 分页: 分页参数放在 ThreadLocal 里,agent 通过反射读取。
 */
class PageHelperPageTest extends ItBase {

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    @DisplayName("第一页只追加单个 LIMIT")
    void firstPageAppendsSingleLimit() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageHelper.startPage(1, 2);
            session.getMapper(RawUserMapper.class).findByRemark("plain");
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByRemark");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(sql.contains("LIMIT 2"), "缺少 LIMIT: " + sql);
    }

    @Test
    @DisplayName("第二页追加 offset, size")
    void secondPageAppendsOffset() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageHelper.startPage(3, 2);
            session.getMapper(RawUserMapper.class).findByRemark("plain");
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByRemark");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(sql.contains("LIMIT 4, 2"), "offset 不正确: " + sql);
    }

    @Test
    @DisplayName("未启用分页时不追加 LIMIT")
    void withoutStartPageNoLimit() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.getMapper(RawUserMapper.class).findByRemark("plain");
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("findByRemark");
        assertNotNull(sql, "未打印 SQL:\n" + LogRecords.dump());
        assertTrue(!sql.contains("LIMIT"), "不应出现 LIMIT: " + sql);
    }
}
