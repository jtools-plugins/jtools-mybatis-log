package com.lhstack.jtools.mybatis.it.mapper;

import org.apache.ibatis.annotations.Select;

/**
 * 该 mapper 在 it-config.properties 的 excludePackages 中被排除,不应打印 SQL。
 */
public interface ExcludedMapper {

    @Select("SELECT count(1) FROM t_user")
    int countAll();
}
