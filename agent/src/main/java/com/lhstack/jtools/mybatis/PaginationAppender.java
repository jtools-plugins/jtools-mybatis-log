package com.lhstack.jtools.mybatis;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 在 Executor 层补出分页插件尚未改写的 ORDER BY 与分页片段。
 * <p>
 * mybatis-plus 与 pagehelper 都是可选依赖,这里全部通过反射访问:
 * 未引入对应框架的项目不会触发任何类解析,避免每条 SQL 都抛一次
 * {@code NoClassDefFoundError}。
 * <p>
 * 注意: 分页插件在进入本类之后才改写 BoundSql,本类拿不到改写结果,
 * 因此只能按分页参数推算。分页语法跟随配置的 sqlFormatType:
 * PlSql 按 Oracle 11g 的 ROWNUM 嵌套查询输出,其余方言保持 MySQL 的 LIMIT。
 * count 语句与主查询共用同一分页参数,其输出的分页片段仅供参考。
 */
final class PaginationAppender {

    private static final String IPAGE_INTERFACE = "com.baomidou.mybatisplus.core.metadata.IPage";

    private static final String PAGE_HELPER_CLASS = "com.github.pagehelper.PageHelper";

    /** sql-formatter 的 PlSql 就是 Oracle 方言,分页语法与格式化选项使用同一个配置。 */
    private static final String ORACLE_DIALECT = "PlSql";

    /** 三态: null 未探测, TRUE/FALSE 已探测。避免重复 Class.forName。 */
    private static volatile Boolean pageHelperPresent;

    private static volatile Method getLocalPageMethod;

    private PaginationAppender() {
    }

    static String append(String sql, Object parameter, String dialectName) {
        StringBuilder sb = new StringBuilder(sql);
        if (!appendMybatisPlusPage(sb, parameter, dialectName)) {
            appendPageHelperPage(sb, dialectName);
        }
        return sb.toString();
    }

    // region mybatis-plus

    /**
     * @return 是否命中 mybatis-plus 分页参数
     */
    private static boolean appendMybatisPlusPage(StringBuilder sb, Object parameter, String dialectName) {
        Object page = resolvePage(parameter);
        if (page == null) {
            return false;
        }
        try {
            appendOrderBy(sb, page);
            appendPage(sb, toLong(invoke(page, "getCurrent")), toLong(invoke(page, "getSize")), dialectName);
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
        if (parameter instanceof Map) {
            return resolvePageFromValues((Map<?, ?>) parameter);
        }
        return null;
    }

    /**
     * 遍历参数值查找分页对象,而不是按固定键名取值。
     * <p>
     * 两个原因: MyBatis 的 ParamMap 在键不存在时抛 BindingException 而非返回 null,
     * 按键取值会让不含该键的语句整条无法渲染; 分页参数的键名又取决于 mapper 方法签名,
     * BaseMapper.selectPage 的 page 参数没有 @Param 注解,键名是 param1 而不是 page。
     */
    private static Object resolvePageFromValues(Map<?, ?> parameter) {
        for (Object value : parameter.values()) {
            if (isPage(value)) {
                return value;
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

    private static void appendPageHelperPage(StringBuilder sb, String dialectName) {
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
            appendPage(sb, toLong(invoke(localPage, "getPageNum")), pageSize, dialectName);
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

    /**
     * 按当前页与页大小补分页。size 无效时不补,避免把不分页的语句改成非法 SQL。
     */
    private static void appendPage(StringBuilder sb, long current, long size, String dialectName) {
        if (size <= 0) {
            return;
        }
        long page = current <= 1 ? 1 : current;
        long offset = (page - 1) * size;
        if (isOracle(dialectName)) {
            appendOraclePage(sb, offset, offset + size);
            return;
        }
        appendLimit(sb, offset, size, page == 1);
    }

    private static boolean isOracle(String dialectName) {
        return dialectName != null && ORACLE_DIALECT.equalsIgnoreCase(dialectName.trim());
    }

    /**
     * 与 MyBatis-Plus OracleDialect 一致的 11g 写法:
     * 内层 ROWNUM 限制结束行,外层过滤已跳过的行。
     */
    private static void appendOraclePage(StringBuilder sb, long offset, long endRow) {
        String originalSql = sb.toString();
        sb.setLength(0);
        sb.append("SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( ")
                .append(originalSql)
                .append(" ) TMP WHERE ROWNUM <= ")
                .append(endRow)
                .append(") WHERE ROW_ID > ")
                .append(offset);
    }

    private static void appendLimit(StringBuilder sb, long offset, long size, boolean firstPage) {
        sb.append(" LIMIT ");
        if (firstPage) {
            sb.append(size);
        } else {
            sb.append(offset).append(", ").append(size);
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
