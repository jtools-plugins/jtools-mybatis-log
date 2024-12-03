package com.jtools.mybatislog;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MethodSqlPrintIntercept {

    @RuntimeType
    public static Object intercept(@Origin Method method, @This Configuration configuration, @SuperCall Callable<?> callable) throws Exception {
        Object result = callable.call();
        //如果是newExecutor
        if ("newExecutor".equals(method.getName()) && !(result instanceof ExecutorWrapper)) {
            return new ExecutorWrapper(configuration, (Executor) result);
        }
        return result;
    }
}
