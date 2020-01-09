package com.tang.flowable.demo.domain;

import lombok.Data;
import org.flowable.task.api.Task;

import java.util.Date;

/**
 * author: tangj <br>
 * date: 2019-04-09 11:21 <br>
 * description:
 */
@Data
public class TaskVO {

    private Date createTime;

    private Date endTime;

    private String id;

    private String starter;

    private Vacation vacation;

}
