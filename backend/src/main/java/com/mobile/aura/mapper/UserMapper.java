// file: com/example/demo2/mapper/UserMapper.java
package com.mobile.aura.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
