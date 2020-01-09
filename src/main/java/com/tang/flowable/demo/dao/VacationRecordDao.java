package com.tang.flowable.demo.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * author: tangj <br>
 * date: 2019-04-04 16:44 <br>
 * description:
 */
@Repository
public interface VacationRecordDao {

    @Insert("insert into bbd_vacation_record (user_id, start, end) value (#{userId},#{start},#{end})")
    void insert(@Param("userId") String userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
