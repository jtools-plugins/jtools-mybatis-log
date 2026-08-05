package com.lhstack.jtools.mybatis.it;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lhstack.jtools.mybatis.it.entity.User;
import com.lhstack.jtools.mybatis.it.mapper.UserMapper;
import com.lhstack.jtools.mybatis.it.support.ItBase;
import com.lhstack.jtools.mybatis.it.support.LogRecords;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * mybatis-plus 场景。覆盖 ParamMap 键名不是 page 的回归:
 * BaseMapper 的 page 参数没有 @Param 注解,键名为 param1,
 * 且 ParamMap.get 在键不存在时抛 BindingException。
 */
class MybatisPlusPageTest extends ItBase {

    @Override
    protected void registerInterceptors(MybatisConfiguration configuration) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);
    }

    @Test
    @DisplayName("selectList: ParamMap 不含 page 键时仍能打印 SQL")
    void selectListWithoutPageKey() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> users = mapper.selectList(new QueryWrapper<User>().gt("age", 20));
            assertEquals(3, users.size());
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("UserMapper.selectList");
        assertNotNull(sql, "selectList 应打印 SQL, 实际日志:\n" + LogRecords.dump());
        assertTrue(sql.contains("SELECT"), "SQL 内容异常: " + sql);
        assertTrue(sql.contains("20"), "参数值应被回填: " + sql);
        assertFalse(sql.contains("?"), "占位符应全部被替换: " + sql);
    }

    @Test
    @DisplayName("selectPage: page 键名为 param1 也能识别并输出 LIMIT")
    void selectPageResolvesPageByValue() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            Page<User> page = new Page<>(2, 2);
            mapper.selectPage(page, new QueryWrapper<User>().gt("age", 10));
            assertEquals(4, page.getTotal());
        }
        assertNoAgentError();

        // mybatis-plus 的 selectPage 复用 selectList 的 MappedStatement,
        // 因此 agent 打印的 statement id 是 selectList
        String sql = LogRecords.lastSqlOf("UserMapper.selectList");
        assertNotNull(sql, "selectPage 应打印 SQL, 实际日志:\n" + LogRecords.dump());
        assertTrue(sql.contains("LIMIT 2, 2"),
                "第2页每页2条应输出 LIMIT 2, 2, 实际: " + sql);
    }

    @Test
    @DisplayName("selectPage: 排序参数输出到 ORDER BY")
    void selectPageAppendsOrderBy() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            Page<User> page = new Page<>(1, 10);
            page.addOrder(OrderItem.desc("age"), OrderItem.asc("name"));
            mapper.selectPage(page, new QueryWrapper<User>().gt("age", 10));
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("UserMapper.selectList");
        assertNotNull(sql, "selectPage 应打印 SQL, 实际日志:\n" + LogRecords.dump());
        assertTrue(sql.contains("ORDER BY age DESC, name ASC"),
                "应输出多列排序, 实际: " + sql);
    }

    @Test
    @DisplayName("selectById: 单参数不受影响")
    void selectByIdStillWorks() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            User user = mapper.selectById(1L);
            assertEquals("alice", user.getName());
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("UserMapper.selectById");
        assertNotNull(sql, "selectById 应打印 SQL");
        assertTrue(sql.contains("1"), "主键应被回填: " + sql);
    }

    @Test
    @DisplayName("insert / update: 写操作同样打印")
    void writeOperationsArePrinted() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            User user = new User();
            user.setId(99L);
            user.setName("dave");
            user.setAge(60);
            user.setRemark("new");
            assertEquals(1, mapper.insert(user));
            session.commit();
        }
        assertNoAgentError();

        String sql = LogRecords.lastSqlOf("UserMapper.insert");
        assertNotNull(sql, "insert 应打印 SQL, 实际日志:\n" + LogRecords.dump());
        assertTrue(sql.contains("'dave'"), "字符串参数应带引号: " + sql);
    }
}
