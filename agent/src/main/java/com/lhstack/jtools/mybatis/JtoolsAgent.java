package com.lhstack.jtools.mybatis;

import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.*;

public class JtoolsAgent {

    /**
     * 增强时原方法被改名为 {@code newExecutor$agent$<类名>}, 该中缀同时用作
     * "本类已被增强" 的标记, 生成与检测必须使用同一个常量。
     */
    private static final String AGENT_METHOD_INFIX = "$agent$";

    private static final Set<String> ENHANCES = new HashSet<>();

    static {
        ENHANCES.add("org/apache/ibatis/session/Configuration");
        ENHANCES.add("com/baomidou/mybatisplus/core/MybatisConfiguration");
    }

    public static void premain(String args, Instrumentation inst) {
        try {
            if (args == null || args.isEmpty()) {
                System.err.println("[jtools-mybatis-log] Agent args is empty, skip initialization");
                return;
            }
            String[] argArray = args.split(",");
            if (argArray.length < 2) {
                System.err.println("[jtools-mybatis-log] Invalid agent args format, skip initialization");
                return;
            }
            
            String configPath = new String(Base64.getDecoder().decode(argArray[1]), StandardCharsets.UTF_8);
            File file = new File(configPath);
            Properties p = new Properties();
            if (!file.exists() || !file.isFile()) {
                System.out.println("[jtools-mybatis-log] Config file not found: " + file.getAbsolutePath());
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    p.load(fis);
                }
            }

            final String ansiCode = argArray[0];
            final String excludePkgs = p.getProperty("excludePackages", "");
            final String sqlType = p.getProperty("sqlFormatType", "MySql");
            final boolean sqlFormatEnable = Boolean.parseBoolean(p.getProperty("sqlFormatEnable", "true"));

            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                         ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                    if (className == null || !ENHANCES.contains(className)) {
                        return null;
                    }

                    try {
                        return enhance(loader, className, sqlType, ansiCode, excludePkgs, classfileBuffer);
                    } catch (Throwable e) {
                        System.err.println("[jtools-mybatis-log] Transform error for " + className + ": " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                }

                /**
                 * 目标类原本不存在带该中缀的方法, 出现即说明已被另一份 agent 增强过。
                 */
                private boolean isAlreadyEnhanced(CtClass ctClass) {
                    for (CtMethod declared : ctClass.getDeclaredMethods()) {
                        if (declared.getName().contains(AGENT_METHOD_INFIX)) {
                            return true;
                        }
                    }
                    return false;
                }

                private byte[] enhance(ClassLoader loader, String className, String sqlType,
                                        String ansiCode, String excludePackages, byte[] originalBytecode) {
                    String classPath = className.replace("/", ".");
                    ClassPool pool = new ClassPool(true);

                    try {
                        if (loader != null) {
                            pool.appendClassPath(new LoaderClassPath(loader));
                        }
                        pool.appendSystemPath();

                        CtClass ctClass = pool.makeClass(new ByteArrayInputStream(originalBytecode));

                        // 同一 JVM 可能挂载多份本 agent: 开发本插件时, IDE 中已启用的插件
                        // 会向测试进程再注入一份。JVM 把上一个 transformer 的产物交给下一个,
                        // 重复增强会产生同名同签名的方法, 导致
                        // ClassFormatError: Duplicate method name, 整个类无法加载。
                        if (isAlreadyEnhanced(ctClass)) {
                            System.out.println("[jtools-mybatis-log] Already enhanced by another agent instance, skip: " + classPath);
                            ctClass.detach();
                            return null;
                        }

                        boolean modified = false;
                        CtMethod[] methods = ctClass.getDeclaredMethods();
                        for (CtMethod method : methods) {
                            try {
                                if ("newExecutor".equals(method.getName()) &&
                                        method.getReturnType().getName().equals("org.apache.ibatis.executor.Executor")) {

                                    String agentMethodName = method.getName() + AGENT_METHOD_INFIX + ctClass.getName().replace(".", "$");
                                    method.setName(agentMethodName);

                                    CtMethod methodCopy = CtNewMethod.copy(method, "newExecutor", ctClass, new ClassMap());
                                    String body = String.format(
                                            "{ return ($r)new com.lhstack.jtools.mybatis.ExecutorWrapper($0, %s($$), \"%s\", \"%s\", \"%s\", %s); }",
                                            agentMethodName, sqlType, ansiCode, excludePackages, sqlFormatEnable
                                    );
                                    methodCopy.setBody(body);
                                    ctClass.addMethod(methodCopy);
                                    modified = true;
                                    System.out.println("[jtools-mybatis-log] Enhanced: " + classPath + ".newExecutor");
                                }
                            } catch (Exception e) {
                                System.err.println("[jtools-mybatis-log] Failed to enhance method " + method.getName() + ": " + e.getMessage());
                            }
                        }

                        if (modified) {
                            byte[] bytecode = ctClass.toBytecode();
                            ctClass.detach();
                            return bytecode;
                        }

                        ctClass.detach();
                        return null;
                    } catch (Throwable e) {
                        System.err.println("[jtools-mybatis-log] Enhance error for " + classPath + ": " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                }
            });

            System.out.println("[jtools-mybatis-log] Agent initialized successfully");
        } catch (Throwable e) {
            System.err.println("[jtools-mybatis-log] Agent initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
