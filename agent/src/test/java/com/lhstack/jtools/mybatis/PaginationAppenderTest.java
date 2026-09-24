package com.lhstack.jtools.mybatis;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 分页片段只依赖分页参数和配置方言,不经过字节码增强。
 */
class PaginationAppenderTest {

    private static final String SQL = "SELECT id FROM t_user WHERE age > 10";

    @Test
    @DisplayName("PlSql 第二页输出 Oracle ROWNUM 分页")
    void oracleSecondPageUsesRownum() {
        Page<Object> page = new Page<>(2, 2);

        String actual = PaginationAppender.append(SQL, pageParameter(page), "PlSql");

        assertEquals("SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( "
                + SQL
                + " ) TMP WHERE ROWNUM <= 4) WHERE ROW_ID > 2", actual);
        assertFalse(actual.contains("LIMIT"), actual);
    }

    @Test
    @DisplayName("PlSql 第一页的起始行是 0")
    void oracleFirstPageStartsAtZero() {
        Page<Object> page = new Page<>(1, 10);

        String actual = PaginationAppender.append(SQL, pageParameter(page), "plsql");

        assertEquals("SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( "
                + SQL
                + " ) TMP WHERE ROWNUM <= 10) WHERE ROW_ID > 0", actual);
    }

    @Test
    @DisplayName("非 Oracle 方言保持 MySQL LIMIT")
    void mysqlKeepsLimit() {
        Page<Object> page = new Page<>(3, 2);

        String actual = PaginationAppender.append(SQL, pageParameter(page), "MySql");

        assertEquals(SQL + " LIMIT 4, 2", actual);
    }

    @Test
    @DisplayName("没有分页参数时不改 SQL")
    void withoutPageKeepsOriginalSql() {
        String actual = PaginationAppender.append(SQL, Collections.singletonMap("age", 10), "PlSql");

        assertEquals(SQL, actual);
    }

    private static Map<String, Object> pageParameter(Page<Object> page) {
        Map<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("param1", page);
        return parameter;
    }
}
