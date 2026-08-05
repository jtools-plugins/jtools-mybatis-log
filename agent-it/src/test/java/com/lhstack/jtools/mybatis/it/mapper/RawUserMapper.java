package com.lhstack.jtools.mybatis.it.mapper;

import com.lhstack.jtools.mybatis.it.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 纯 MyBatis mapper,用于验证占位符扫描与字面量渲染。
 */
public interface RawUserMapper {

    @Select("SELECT id, name, age, remark FROM t_user WHERE name = #{name}")
    List<User> findByName(@Param("name") String name);

    /**
     * remark 里的 {@code ?} 位于字符串字面量内,不是占位符;
     * 若按裸字符统计会让 #{age} 的值错位到该位置。
     */
    @Select("SELECT id, name, age, remark FROM t_user WHERE remark = 'a?b' AND age > #{age}")
    List<User> findByLiteralQuestion(@Param("age") int age);

    /**
     * 行注释与块注释里的 {@code ?} 同样不是占位符。
     */
    @Select("SELECT id, name, age, remark FROM t_user /* who? */ WHERE name = #{name} -- why?\n AND age > #{age}")
    List<User> findWithComments(@Param("name") String name, @Param("age") int age);

    @Select("SELECT id, name, age, remark FROM t_user WHERE remark = #{remark}")
    List<User> findByRemark(@Param("remark") String remark);

    @Select("SELECT count(1) FROM t_user WHERE (#{createTime} IS NULL OR #{createTime} > now())")
    int countByTime(@Param("createTime") Date createTime);
}
