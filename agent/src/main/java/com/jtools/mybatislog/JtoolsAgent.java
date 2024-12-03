package com.jtools.mybatislog;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.ibatis.session.Configuration;

import java.lang.instrument.Instrumentation;

public class JtoolsAgent {

    public static void premain(String args, Instrumentation inst) {
        AgentBuilder.Default.of().type(typeDefinitions -> {
                    if (typeDefinitions != null) {
                        return typeDefinitions.isAssignableTo(Configuration.class);
                    }
                    return false;
                })
                .transform((builder, typeDescription, classLoader, javaModule) -> {
                    return builder.method(ElementMatchers.any())
                            .intercept(MethodDelegation.to(MethodSqlPrintIntercept.class));
                }).installOn(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
