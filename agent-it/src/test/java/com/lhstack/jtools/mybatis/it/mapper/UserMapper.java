package com.lhstack.jtools.mybatis.it.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhstack.jtools.mybatis.it.entity.User;

/**
 * 走 mybatis-plus BaseMapper: selectPage / selectList 的参数是 ParamMap,
 * 且 page 参数没有 @Param 注解,键名为 param1。
 */
public interface UserMapper extends BaseMapper<User> {
}
