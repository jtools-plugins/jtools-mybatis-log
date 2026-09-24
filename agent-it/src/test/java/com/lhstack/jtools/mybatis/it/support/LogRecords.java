package com.lhstack.jtools.mybatis.it.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 收集 agent 通过 MyBatis 日志门面输出的内容。
 * <p>
 * agent 把 SQL 打在 warn 级别,格式为:
 * {@code statementId\r\n<ansi>sql<reset>\r\n执行耗时: Nms}
 */
public final class LogRecords {

    public static final class Entry {
        public final String level;
        public final String message;
        public final Throwable error;

        Entry(String level, String message, Throwable error) {
            this.level = level;
            this.message = message;
            this.error = error;
        }
    }

    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<Entry>());

    private LogRecords() {
    }

    public static void add(String level, String message, Throwable error) {
        ENTRIES.add(new Entry(level, message, error));
    }

    public static void clear() {
        ENTRIES.clear();
    }

    /** agent 打印的 SQL 日志(按出现顺序) */
    public static List<Entry> sqlEntries() {
        List<Entry> result = new ArrayList<Entry>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                if (entry.message != null && entry.message.contains("执行耗时")) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /** agent 自身的错误日志,用于断言没有发生 gen sql failure */
    public static List<String> agentErrors() {
        List<String> result = new ArrayList<String>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                if ("error".equals(entry.level) && entry.message != null
                        && entry.message.contains("[jtools-mybatis-log]")) {
                    result.add(entry.message + describe(entry.error));
                }
            }
        }
        return result;
    }

    private static String describe(Throwable error) {
        return error == null ? "" : " <- " + error.getClass().getName() + ": " + error.getMessage();
    }

    /** 从日志消息中取出被 ansi 包裹的 SQL 主体 */
    public static String sqlOf(Entry entry) {
        String message = entry.message;
        int start = message.indexOf('m', message.indexOf("\u001B["));
        int end = message.indexOf("\u001B[0m");
        if (start < 0 || end < 0 || start + 1 > end) {
            throw new IllegalStateException("日志格式不符合预期: " + message);
        }
        return message.substring(start + 1, end);
    }

    public static String statementIdOf(Entry entry) {
        return entry.message.substring(0, entry.message.indexOf("\r\n"));
    }

    /** 找到指定 statement 的最后一条 SQL 日志 */
    public static String lastSqlOf(String statementIdSuffix) {
        List<Entry> entries = sqlEntries();
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (statementIdOf(entries.get(i)).endsWith(statementIdSuffix)) {
                return sqlOf(entries.get(i));
            }
        }
        return null;
    }

    public static String dump() {
        StringBuilder sb = new StringBuilder();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                sb.append('[').append(entry.level).append("] ").append(entry.message).append('\n');
            }
        }
        return sb.toString();
    }
}
