package com.lhstack.jtools.mybatis;

import java.util.Arrays;

/**
 * 扫描 SQL 中真正的参数占位符位置。
 * <p>
 * 字符串字面量、行注释({@code --})和块注释({@code /* *}{@code /})内部的 {@code ?}
 * 不是占位符,按裸字符统计会让其后所有参数错位。
 */
final class PlaceholderScanner {

    private static final int[] EMPTY = new int[0];

    private PlaceholderScanner() {
    }

    /**
     * @return 占位符在 SQL 中的字符下标,按出现顺序排列
     */
    static int[] positions(String sql) {
        if (sql == null || sql.isEmpty()) {
            return EMPTY;
        }
        int[] positions = new int[16];
        int count = 0;
        int index = 0;
        int length = sql.length();
        while (index < length) {
            char current = sql.charAt(index);
            if (current == '\'' || current == '"' || current == '`') {
                index = skipQuoted(sql, index, current);
            } else if (isLineCommentStart(sql, index)) {
                index = skipLineComment(sql, index);
            } else if (isBlockCommentStart(sql, index)) {
                index = skipBlockComment(sql, index);
            } else {
                if (current == '?') {
                    if (count == positions.length) {
                        positions = Arrays.copyOf(positions, count * 2);
                    }
                    positions[count++] = index;
                }
                index++;
            }
        }
        return count == positions.length ? positions : Arrays.copyOf(positions, count);
    }

    private static boolean isLineCommentStart(String sql, int index) {
        return sql.charAt(index) == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-';
    }

    private static boolean isBlockCommentStart(String sql, int index) {
        return sql.charAt(index) == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*';
    }

    /**
     * 同时支持重复引号({@code ''})和反斜杠({@code \'})两种转义写法,
     * 前者是标准 SQL,后者是 MySQL/PostgreSQL 常见方言。
     */
    private static int skipQuoted(String sql, int start, char quote) {
        int length = sql.length();
        int index = start + 1;
        while (index < length) {
            char current = sql.charAt(index);
            if (current == '\\' && quote != '`') {
                index += 2;
                continue;
            }
            if (current == quote) {
                if (index + 1 < length && sql.charAt(index + 1) == quote) {
                    index += 2;
                    continue;
                }
                return index + 1;
            }
            index++;
        }
        return length;
    }

    private static int skipLineComment(String sql, int start) {
        int length = sql.length();
        int index = start + 2;
        while (index < length && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private static int skipBlockComment(String sql, int start) {
        int length = sql.length();
        int index = start + 2;
        while (index + 1 < length) {
            if (sql.charAt(index) == '*' && sql.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return length;
    }
}
