package com.jtools.mybatislog;

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
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    try {
                        if (className != null && ENHANCES.contains(className)) {
                            return enhance(loader, className, Optional.ofNullable(p.getProperty("sqlFormatType")).orElse("Mysql"));
                        }
                    } catch (Throwable e) {
                        System.err.println("[jtools-mybatis-log] Transform error for class " + className + ": " + e.getMessage());
                    }
                    return classfileBuffer;
                }

                private CtClass findClassInClassPool(ClassLoader loader, String classPath, boolean firstTry) {
                    ClassPool pool = ClassPool.getDefault();
                    CtClass ctClass = null;
                    try {
                        ctClass = pool.get(classPath);
                    } catch (NotFoundException e) {
                        if (firstTry && loader != null) {
                            pool.appendClassPath(new LoaderClassPath(loader));
                            ctClass = findClassInClassPool(loader, classPath, false);
                        }
                    }
                    return ctClass;
                }

                private byte[] enhance(ClassLoader loader, String className, String sqlType) {
                    String classPath = className.replaceAll("/", ".");
                    CtClass ctClass = this.findClassInClassPool(loader, classPath, true);
                    if (ctClass == null) {
                        System.err.println("[jtools-mybatis-log] Cannot find class in pool: " + classPath);
                        return null;
                    }
                    try {
                        // 只对本类方法进行拦截，不处理父类方法
                        CtMethod[] methods = ctClass.getDeclaredMethods();
                        for (CtMethod method : methods) {
                            if ("newExecutor".equals(method.getName()) && method.getReturnType().getName().equals("org.apache.ibatis.executor.Executor")) {
                                CtMethod methodCopy = CtNewMethod.copy(method, ctClass, new ClassMap());
                                String agentMethodName = method.getName() + "$agent$" + ctClass.getName().replace(".", "$");
                                method.setName(agentMethodName);
                                String excludePackages = p.getProperty("excludePackages");
                                methodCopy.setBody(String.format("{\n return ($r)new com.jtools.mybatislog.ExecutorWrapper($0,%s($),\"%s\",\"%s\",\"%s\");\n}", agentMethodName, sqlType, argArray[0], excludePackages == null ? "" : excludePackages));
                                ctClass.addMethod(methodCopy);
                            }
                        }
                        byte[] bytecode = ctClass.toBytecode();
                        ctClass.detach(); // Release memory
                        return bytecode;
                    } catch (Throwable e) {
                        System.err.println("[jtools-mybatis-log] Enhance error for class " + classPath + ": " + e.getMessage());
                        return null;
                    }
                }
            }, true); // Set canRetransform to true
        } catch (Throwable e) {
            System.err.println("[jtools-mybatis-log] Agent initialization error: " + e.getMessage());
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
