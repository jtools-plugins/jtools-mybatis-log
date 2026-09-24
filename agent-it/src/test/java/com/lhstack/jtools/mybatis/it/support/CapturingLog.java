package com.lhstack.jtools.mybatis.it.support;

import org.apache.ibatis.logging.Log;

/**
 * 注入到 MyBatis LogFactory 的日志实现,把 agent 的输出转存到 {@link LogRecords}。
 * <p>
 * 必须在 ExecutorWrapper 类初始化之前调用 LogFactory.useCustomLogging 注册,
 * 因为它的 LOGGER 是静态字段。
 */
public class CapturingLog implements Log {

    public CapturingLog(String name) {
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void error(String s, Throwable e) {
        LogRecords.add("error", s, e);
    }

    @Override
    public void error(String s) {
        LogRecords.add("error", s, null);
    }

    @Override
    public void debug(String s) {
        LogRecords.add("debug", s, null);
    }

    @Override
    public void trace(String s) {
        LogRecords.add("trace", s, null);
    }

    @Override
    public void warn(String s) {
        LogRecords.add("warn", s, null);
    }
}
