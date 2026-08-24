package com.token.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.token.erp.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
