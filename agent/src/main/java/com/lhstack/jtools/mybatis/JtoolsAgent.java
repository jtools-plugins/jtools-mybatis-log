package com.lhstack.jtools.mybatis;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public class JtoolsAgent {

    private static final Set<String> ENHANCES = new HashSet<>();

    static {
        ENHANCES.add("org/apache/ibatis/session/Configuration");
        ENHANCES.add("com/baomidou/mybatisplus/core/MybatisConfiguration");
    }
    public static void premain(String args, Instrumentation inst) {
        try {
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    try {
                        if (ENHANCES.contains(className)) {
                            return enhance(loader, className, "Mysql");
                        }
                    } catch (Throwable ignore) {

                    }
                    return classfileBuffer;
                }


                private CtClass findClassInClassPool(ClassLoader loader, String classPath, boolean firstTry) {
                    ClassPool pool = ClassPool.getDefault();
                    CtClass ctClass = null;
                    try {
                        ctClass = pool.get(classPath);
                    } catch (NotFoundException e) {
                        if (firstTry) {
                            pool.appendClassPath(new LoaderClassPath(loader));
                            ctClass = findClassInClassPool(loader, classPath, false);
                        }
                    }
                    return ctClass;
                }

                private byte[] enhance(ClassLoader loader, String className, String sqlType) {
                    String classPath = className.replaceAll("/", ".");
                    CtClass ctClass = this.findClassInClassPool(loader, classPath, true);
                    try {
                        // 只对本类方法进行拦截，不处理父类方法
                        CtMethod[] methods = ctClass.getDeclaredMethods();
                        for (CtMethod method : methods) {
                            if ("newExecutor".equals(method.getName()) && method.getReturnType().getName().equals("org.apache.ibatis.executor.Executor")) {
                                CtMethod methodCopy = CtNewMethod.copy(method, ctClass, new ClassMap());
                                String agentMethodName = method.getName() + "$agent$" + ctClass.getName().replace(".", "$");
                                method.setName(agentMethodName);
                                methodCopy.setBody(String.format("{\n return ($r)new com.lhstack.jtools.mybatis.ExecutorWrapper($0,%s($$),\"%s\",\"%s\");\n}", agentMethodName, sqlType, args));
                                ctClass.addMethod(methodCopy);
                            }
                        }
                        return ctClass.toBytecode();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Throwable ignore) {

        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
