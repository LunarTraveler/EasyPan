package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcu.entity.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}