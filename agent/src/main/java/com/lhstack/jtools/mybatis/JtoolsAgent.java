package com.lhstack.jtools.mybatis;

import javassist.*;

import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.*;

public class JtoolsAgent {

    private static final Set<String> ENHANCES = new HashSet<>();
    static {
        ENHANCES.add("org/apache/ibatis/session/Configuration");
        ENHANCES.add("com/baomidou/mybatisplus/core/MybatisConfiguration");
    }

    public static void premain(String args, Instrumentation inst) {
        try {
            String[] argArray = args.split(",");
            String configPath = new String(Base64.getDecoder().decode(argArray[1]), StandardCharsets.UTF_8);
            File file = new File(configPath);
            Properties p = new Properties();
            if(!file.exists() || !file.isFile()){
                System.out.printf("jtools-mybatis-log配置文件不存在,配置地址: %s%n", file.getAbsolutePath());
            }else {
                try(FileInputStream fis = new FileInputStream(file)){
                    p.load(fis);
                }
            }
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    try {
                        if (ENHANCES.contains(className)) {
                            return enhance(loader, className, Optional.ofNullable(p.getProperty("sqlFormatType")).orElse("Mysql"));
                        }
                    } catch (Throwable ignore) {
                        ignore.printStackTrace();
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
                                methodCopy.setBody(String.format("{\n return ($r)new com.lhstack.jtools.mybatis.ExecutorWrapper($0,%s($$),\"%s\",\"%s\",\"%s\");\n}", agentMethodName, sqlType, argArray[0], p.getProperty("excludePackages")));
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
