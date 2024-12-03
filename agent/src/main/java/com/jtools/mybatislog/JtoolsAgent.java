package com.jtools.mybatislog;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class JtoolsAgent {
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if ("org/apache/ibatis/session/Configuration".equals(className)) {
                    return enhance(loader, className);
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

            private byte[] enhance(ClassLoader loader, String className) {
                String classPath = className.replaceAll("/", ".");
                CtClass ctClass = this.findClassInClassPool(loader, classPath, true);
                try {
                    // 只对本类方法进行拦截，不处理父类方法
                    CtMethod[] methods = ctClass.getDeclaredMethods();
                    for (CtMethod method : methods) {
                        if ("newExecutor".equals(method.getName())) {
                            CtMethod methodCopy = CtNewMethod.copy(method, ctClass, new ClassMap());
                            String agentMethodName = method.getName() + "$agent";
                            method.setName(agentMethodName);
                            methodCopy.setBody(String.format("{\n return ($r)new com.jtools.mybatislog.ExecutorWrapper($0,%s($$));\n}", agentMethodName));
                            ctClass.addMethod(methodCopy);
                        }
                    }
                    return ctClass.toBytecode();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
