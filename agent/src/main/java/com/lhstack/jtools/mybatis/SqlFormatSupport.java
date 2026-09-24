package com.lhstack.jtools.mybatis;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;

/**
 * SQL 格式化。sql-formatter 已随 agent 一起 shade,运行期必然可用。
 */
final class SqlFormatSupport {

    private SqlFormatSupport() {
    }

    static String format(String sql, String dialectName) {
        return SqlFormatter.of(resolveDialect(dialectName)).format(sql);
    }

    private static Dialect resolveDialect(String dialectName) {
        if (dialectName == null || dialectName.trim().isEmpty()) {
            return Dialect.MySql;
        }
        return Dialect.nameOf(dialectName.trim()).orElse(Dialect.MySql);
    }
}
