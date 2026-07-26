package com.lhstack.jtools.mybatis;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 在 Executor 层补出分页插件尚未改写的 ORDER BY / LIMIT 片段。
 * <p>
 * mybatis-plus 与 pagehelper 都是可选依赖,这里全部通过反射访问:
 * 未引入对应框架的项目不会触发任何类解析,避免每条 SQL 都抛一次
 * {@code NoClassDefFoundError}。
 * <p>
 * 注意: 分页插件在 StatementHandler 层改写 BoundSql,本类拿不到改写结果,
 * 因此只能按分页参数推算。count 语句与主查询共用同一分页参数,
 * 其输出的 LIMIT / ORDER BY 仅供参考。
 */
final class PaginationAppender {

    private static final String IPAGE_INTERFACE = "com.baomidou.mybatisplus.core.metadata.IPage";

    private static final String PAGE_HELPER_CLASS = "com.github.pagehelper.PageHelper";

    /** 三态: null 未探测, TRUE/FALSE 已探测。避免重复 Class.forName。 */
    private static volatile Boolean pageHelperPresent;

    private static volatile Method getLocalPageMethod;

    private PaginationAppender() {
    }

    static String append(String sql, Object parameter) {
        StringBuilder sb = new StringBuilder(sql);
        if (!appendMybatisPlusPage(sb, parameter)) {
            appendPageHelperPage(sb);
        }
        return sb.toString();
    }

    // region mybatis-plus

    /**
     * @return 是否命中 mybatis-plus 分页参数
     */
    private static boolean appendMybatisPlusPage(StringBuilder sb, Object parameter) {
        Object page = resolvePage(parameter);
        if (page == null) {
            return false;
        }
        try {
            appendOrderBy(sb, page);
            appendLimit(sb, toLong(invoke(page, "getCurrent")), toLong(invoke(page, "getSize")));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 只按类型名匹配 IPage,不做 Class.forName,因此无 mybatis-plus 时零开销。
     */
    private static Object resolvePage(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (isPage(parameter)) {
            return parameter;
        }
        if (parameter instanceof java.util.Map) {
            Object candidate = ((java.util.Map<?, ?>) parameter).get("page");
            if (isPage(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isPage(Object candidate) {
        if (candidate == null) {
            return false;
        }
        for (Class<?> type = candidate.getClass(); type != null; type = type.getSuperclass()) {
            if (implementsIPage(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean implementsIPage(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            if (IPAGE_INTERFACE.equals(iface.getName()) || implementsIPage(iface)) {
                return true;
            }
        }
        return false;
    }

    private static void appendOrderBy(StringBuilder sb, Object page) throws Exception {
        Object orders = invoke(page, "orders");
        if (!(orders instanceof List)) {
            return;
        }
        List<?> orderItems = (List<?>) orders;
        if (orderItems.isEmpty()) {
            return;
        }
        sb.append(" ORDER BY ");
        for (int i = 0; i < orderItems.size(); i++) {
            Object orderItem = orderItems.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(invoke(orderItem, "getColumn"));
            sb.append(Boolean.TRUE.equals(invoke(orderItem, "isAsc")) ? " ASC" : " DESC");
        }
    }

    // endregion

    // region pagehelper

    private static void appendPageHelperPage(StringBuilder sb) {
        Method method = localPageMethod();
        if (method == null) {
            return;
        }
        try {
            Object localPage = method.invoke(null);
            if (localPage == null) {
                return;
            }
            long pageSize = toLong(invoke(localPage, "getPageSize"));
            if (pageSize <= 0) {
                return;
            }
            appendLimit(sb, toLong(invoke(localPage, "getPageNum")), pageSize);
        } catch (Throwable ignored) {
            // pagehelper 版本差异导致取不到分页参数时,不影响 SQL 主体输出
        }
    }

    private static Method localPageMethod() {
        if (pageHelperPresent == null) {
            synchronized (PaginationAppender.class) {
                if (pageHelperPresent == null) {
                    resolveLocalPageMethod();
                }
            }
        }
        return Boolean.TRUE.equals(pageHelperPresent) ? getLocalPageMethod : null;
    }

    private static void resolveLocalPageMethod() {
        try {
            Class<?> pageHelper = Class.forName(PAGE_HELPER_CLASS, false, classLoader());
            getLocalPageMethod = pageHelper.getMethod("getLocalPage");
            pageHelperPresent = Boolean.TRUE;
        } catch (Throwable e) {
            pageHelperPresent = Boolean.FALSE;
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : PaginationAppender.class.getClassLoader();
    }

    // endregion

    private static void appendLimit(StringBuilder sb, long current, long size) {
        if (size <= 0) {
            return;
        }
        sb.append(" LIMIT ");
        if (current <= 1) {
            sb.append(size);
        } else {
            sb.append((current - 1) * size).append(", ").append(size);
        }
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
