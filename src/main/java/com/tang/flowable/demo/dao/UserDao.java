package com.tang.flowable.demo.dao;

import com.tang.flowable.demo.domain.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * author: tangj <br>
 * date: 2019-04-04 17:10 <br>
 * description:
 */
@Repository
public interface UserDao {

    @Select("select id,username,role_name from bbd_user where role_name=#{roleName}")
    List<User> getUsersByRoleName(@Param("roleName") String roleName);
}
