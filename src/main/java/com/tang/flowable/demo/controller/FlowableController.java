package com.tang.flowable.demo.controller;

import com.tang.flowable.demo.domain.*;
import com.tang.flowable.demo.domain.*;
import com.tang.flowable.demo.service.FlowableService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * author: tangj <br>
 * date: 2019-04-04 17:28 <br>
 * description: 注意：以下接口的userId在正式环境都应该从鉴权中获取，此处只是方便演示
 */
@Api(value = "流程相关")
@RestController
@RequestMapping("flowable")
public class FlowableController {

    @Autowired
    private FlowableService flowableService;

    @ApiOperation("开始请假流程")
    @PostMapping("start/{userId}")
    public Result<String> startProcessInstance(@PathVariable("userId") Long userId, @RequestBody Vacation vacation) {
        flowableService.startProcessInstance(userId, vacation);
        return new Result<>();
    }

    @ApiOperation("获取我的申请")
    @PostMapping("process/me/{userId}")
    public Result<PageInfo<ProcessVo>> getMyProcess(@PathVariable("userId") Long userId,
                                                    @RequestParam(value = "status", required = false) Integer status,
                                                    @RequestParam Integer pageNum,
                                                    @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        return new Result<>(flowableService.getMyProcess(userId, status, pageNum, pageSize));
    }

    @ApiOperation("完成任务")
    @PostMapping("task/complete/{taskId}")
    public Result<String> completeTask(@PathVariable("taskId") String taskId, @RequestParam("userId") Long userId, @RequestBody CommentVO comment) {
        flowableService.completeTask(taskId, userId, comment);
        return new Result<>();
    }

    @ApiOperation("获取已处理任务列表")
    @GetMapping("task/complete")
    public Result<PageInfo<TaskVO>> getFinishedTask(@RequestParam Long userId,
                                                    @RequestParam(required = false) Integer min,
                                                    @RequestParam(required = false) Integer max,
                                                    @RequestParam Integer pageNum,
                                                    @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        return new Result<>(flowableService.getFinishedTask(userId, min, max, pageNum, pageSize));
    }


    @ApiOperation("获取待处理任务列表")
    @GetMapping("task/undo")
    public Result<PageInfo<TaskVO>> getUndoTask(@RequestParam Long userId,
                                        @RequestParam(required = false) Integer min,
                                        @RequestParam(required = false) Integer max,
                                        @RequestParam Integer pageNum,
                                        @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        return new Result<>(flowableService.getUndoTask(userId, min, max, pageNum, pageSize));
    }

    @ApiOperation("获取任务审批意见")
    @GetMapping("comments/{taskId}")
    public Result<List<CommentVO>> getTaskComments(@PathVariable String taskId) {
        return new Result<>(flowableService.getTaskComments(taskId));
    }


    @ApiOperation("获取流程追踪图")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "流程实例id", name = "processInstanceId", paramType = "query", dataType = "String"),
            @ApiImplicitParam(value = "图片类型：jpg/png", name = "imageType", paramType = "query", dataType = "String", allowableValues = "jpg,png"),
    })
    @GetMapping("diagram")
    public void getDiagram(@RequestParam String processInstanceId, @RequestParam String imageType, @ApiIgnore HttpServletResponse response)
            throws Exception {
        InputStream inputStream = flowableService.getDiagram(processInstanceId, imageType);

        byte[] b = new byte[1024];
        int len;
        while ((len = inputStream.read(b, 0, 1024)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }

}
