package com.tang.flowable.demo.service;

import com.alibaba.fastjson.JSON;
import com.tang.flowable.demo.dao.UserDao;
import com.tang.flowable.demo.dao.VacationRecordDao;
import com.tang.flowable.demo.domain.CommentVO;
import com.tang.flowable.demo.domain.ProcessVo;
import com.tang.flowable.demo.domain.TaskVO;
import com.tang.flowable.demo.domain.Vacation;
import com.tang.flowable.demo.enums.ProcessStatus;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author: tangj <br>
 * date: 2019-04-03 17:01 <br>
 * description:
 */
@SuppressWarnings("Duplicates")
@Service
@Slf4j
public class FlowableService {

    private static final String processDefinitionKey = "vacation";

    private static final String DATA_VARIABLE_NAME = "data";

    private static final String DURATION_VARIABLE_NAME = "duration";

    private static final String APPROVED_VARIABLE_NAME = "approve";

    private static final String INITIATOR_VARIABLE_NAME = "initiator";

    private static final String STATUS_VARIABLE_NAME = "status";


    /**
     * 用于部署流程定义的相关操作（因为我们是使用的spring boot starter自动部署流程定义，所以不怎么需要使用这个api）
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * 它用于管理（创建，更新，删除，查询……）组与用户
     */
    @Autowired
    private IdentityService identityService;

    /**
     * 用于启动流程定义的新流程实例
     */
    @Autowired
    private RuntimeService runtimeService;

    /**
     * 历史数据相关的查询操作
     */
    @Autowired
    private HistoryService historyService;

    /**
     * 所有任务相关的东西
     * 查询分派给用户或组的任务
     * 创建独立运行(standalone)任务。这是一种没有关联到流程实例的任务。
     * 决定任务的执行用户(assignee)，或者将用户通过某种方式与任务关联。
     * 认领(claim)与完成(complete)任务。
     */
    @Autowired
    private TaskService taskService;

    /**
     * 表单相关的操作
     */
    @SuppressWarnings("unused")
    @Autowired
    private FormService formService;

    /**
     * 可用于修改流程定义中的部分内容，而不需要重新部署它。
     * 例如可以修改流程定义中一个用户任务的办理人设置，或者修改一个服务任务中的类名。
     */
    @SuppressWarnings("unused")
    @Autowired
    private DynamicBpmnService dynamicBpmnService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private VacationRecordDao vacationRecordDao;

    @Autowired
    private UserDao userDao;


    public void startProcessInstance(Long userId, Vacation vacation) {
        // 用来设置启动流程的人员ID，引擎会自动把用户ID保存到activiti:initiator中
        try {
            identityService.setAuthenticatedUserId(userId.toString());
            Map<String, Object> variables = new HashMap<>();
            variables.put(DATA_VARIABLE_NAME, vacation);
            LocalDate start = vacation.getStart();
            LocalDate end = vacation.getEnd();
            if (start == null || end == null || !end.isAfter(start)) {
                throw new RuntimeException("unacceptable param!");
            }
            Period period = Period.between(start, end);
            variables.put(DURATION_VARIABLE_NAME, period.getDays());

            variables.put(STATUS_VARIABLE_NAME, ProcessStatus.IN_PROCESS);
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
            log.info("process start,instance id:{}", processInstance);
        } finally {
            // 最后要设置null，标准做法
            identityService.setAuthenticatedUserId(null);
        }
    }


    public void completeTask(String taskId, Long userId, CommentVO comment) {
        TaskQuery taskQuery = taskService.createTaskQuery();

        Task task = taskQuery.taskCandidateOrAssigned(userId.toString()).taskId(taskId).singleResult();

        if (task == null) {
            throw new RuntimeException("任务不存在或者你没有执行该任务的权限！");
        }

        //认领任务
        taskService.setAssignee(taskId, userId.toString());

        boolean approve = comment.isApprove();
        Map<String, Object> variables = new HashMap<>();
        variables.put(APPROVED_VARIABLE_NAME, approve);
        if (!approve) {
            variables.put(STATUS_VARIABLE_NAME, ProcessStatus.NOT_PASSED);
        }


        // 评论人 一定要写，不然查看的时候会报错，没有用户
        try {
            identityService.setAuthenticatedUserId(userId.toString());

            taskService.addComment(taskId, task.getProcessInstanceId(), JSON.toJSONString(comment));

            taskService.complete(taskId, variables);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    @SuppressWarnings("unused")
    public void startVacation(DelegateExecution execution) {
        Vacation vacation = (Vacation) execution.getVariable(DATA_VARIABLE_NAME);
        String initiator = (String) execution.getVariable(INITIATOR_VARIABLE_NAME);
        //插入休假记录
        vacationRecordDao.insert(initiator, vacation.getStart(), vacation.getEnd());
        execution.setVariable(STATUS_VARIABLE_NAME, ProcessStatus.PASSED);
    }

    @SuppressWarnings("unused")
    public List<String> getTaskCandidate(DelegateExecution execution) {
        String eventName = execution.getCurrentFlowElement().getName();
        return Optional.ofNullable(userDao.getUsersByRoleName(eventName.replace("审批", "")))
                .orElseThrow(() -> new RuntimeException("没有审核人！"))
                .stream().map(a -> a.getId().toString())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    public String getTaskAssignee(DelegateExecution execution) {
        String eventName = execution.getCurrentFlowElement().getName();
        return userDao.getUsersByRoleName(eventName.replace("审批", ""))
                .stream().map(a -> a.getId().toString()).findFirst().orElseThrow(() -> new RuntimeException("没有审核人！"));
    }


    public PageInfo<TaskVO> getFinishedTask(Long userId, Integer min, Integer max, Integer pageNum, Integer pageSize) {
        HistoricTaskInstanceQuery historicTaskQuery = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .includeProcessVariables()
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc();
        if (userId != null) {
            historicTaskQuery.taskAssignee(userId.toString());
        }
        if (min != null) {
            historicTaskQuery.processVariableValueGreaterThanOrEqual(DURATION_VARIABLE_NAME, min);
        }
        if (max != null) {
            historicTaskQuery.processVariableValueLessThanOrEqual(DURATION_VARIABLE_NAME, max);
        }
        Page<TaskVO> page = new Page<>(pageNum, pageSize);
        int totalSum = historicTaskQuery.list().size();
        page.setReasonable(true);
        page.setTotal(totalSum);
        int startRow = page.getStartRow();
        List<HistoricTaskInstance> tasks = historicTaskQuery.listPage(startRow, pageSize);
        tasks.forEach(task -> {
            TaskVO taskVO = new TaskVO();
            taskVO.setId(task.getId());
            taskVO.setCreateTime(task.getCreateTime());
            taskVO.setEndTime(task.getEndTime());

            Map<String, Object> variables = task.getProcessVariables();
            taskVO.setStarter(variables.get(INITIATOR_VARIABLE_NAME).toString());
            taskVO.setVacation(((Vacation) variables.get(DATA_VARIABLE_NAME)));
            page.add(taskVO);
        });
        return new PageInfo<>(page);
    }

    public PageInfo<TaskVO> getUndoTask(Long userId, Integer min, Integer max, Integer pageNum, Integer pageSize) {
        TaskQuery taskQuery = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                .includeProcessVariables()
                .orderByTaskCreateTime().desc();
        if (userId != null) {
            taskQuery.taskCandidateOrAssigned(userId.toString());
        }
        if (min != null) {
            taskQuery.processVariableValueGreaterThanOrEqual(DURATION_VARIABLE_NAME, min);
        }
        if (max != null) {
            taskQuery.processVariableValueLessThanOrEqual(DURATION_VARIABLE_NAME, max);
        }
        Page<TaskVO> page = new Page<>(pageNum, pageSize);
        int totalSum = taskQuery.list().size();
        page.setReasonable(true);
        page.setTotal(totalSum);
        int startRow = page.getStartRow();
        List<Task> tasks = taskQuery.listPage(startRow, pageSize);
        tasks.forEach(task -> {
            TaskVO taskVO = new TaskVO();
            taskVO.setId(task.getId());
            taskVO.setCreateTime(task.getCreateTime());
            Map<String, Object> variables = task.getProcessVariables();
            taskVO.setStarter(variables.get(INITIATOR_VARIABLE_NAME).toString());
            taskVO.setVacation(((Vacation) variables.get(DATA_VARIABLE_NAME)));
            page.add(taskVO);
        });
        return new PageInfo<>(page);
    }


    public List<CommentVO> getTaskComments(String taskId) {
        return taskService.getTaskComments(taskId).stream()
                .map(a -> JSON.parseObject(a.getFullMessage(), CommentVO.class))
                .collect(Collectors.toList());
    }


    public PageInfo<ProcessVo> getMyProcess(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .includeProcessVariables()
                .startedBy(userId.toString())
                .orderByProcessInstanceStartTime().desc();

        if (status != null) {
            processInstanceQuery.variableValueEquals(STATUS_VARIABLE_NAME, status);
        }
        Page<ProcessVo> page = new Page<>(pageNum, pageSize);
        int totalSum = processInstanceQuery.list().size();
        page.setReasonable(true);
        page.setTotal(totalSum);
        int startRow = page.getStartRow();
        List<HistoricProcessInstance> instances = processInstanceQuery.listPage(startRow, pageSize);

        instances.forEach(instance -> {
            ProcessVo processVo = new ProcessVo();
            processVo.setId(instance.getId());
            processVo.setStart(instance.getStartTime());
            Map<String, Object> variables = instance.getProcessVariables();
            processVo.setData(variables.get(DATA_VARIABLE_NAME));
            processVo.setStatus((Integer) variables.get(STATUS_VARIABLE_NAME));
            page.add(processVo);
        });

        return new PageInfo<>(page);
    }

    public InputStream getDiagram(String processInstanceId, String imageType) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        // ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        // .processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();


        //高亮节点id集合
        List<String> highLightedActivitis = historicActivityInstances.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());
        // 高亮线路id集合
        List<String> highLightedFlows = getHighLightedFlows(bpmnModel, historicActivityInstances);


        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();

        return diagramGenerator.generateDiagram(bpmnModel, imageType, highLightedActivitis,
                highLightedFlows, "黑体", "黑体", "黑体", null, 1.0);
    }

    private List<String> getHighLightedFlows(BpmnModel bpmnModel,
                                             List<HistoricActivityInstance> historicActivityInstances) {

        List<String> highLightedFlows = new ArrayList<>();

        LinkedList<HistoricActivityInstance> hisActInstList = new LinkedList<>(historicActivityInstances);

        List<Process> processes = bpmnModel.getProcesses();
        if (processes != null && !processes.isEmpty()) {
            List<FlowElement> elements = processes.stream().flatMap(a -> a.getFlowElements().stream()).collect(Collectors.toList());
            Map<String, FlowNode> flowNodeMap = getFlowNodes(elements);
            getHighlightedFlows(flowNodeMap, hisActInstList, highLightedFlows);
        }

        return highLightedFlows;
    }

    /**
     * 这是我根据下边官方的写法简化了一下，验证过满足我们自己的情况
     */
    private void getHighlightedFlowsNew(Map<String, FlowNode> flowNodeMap,
                                        LinkedList<HistoricActivityInstance> hisActInstList, List<String> highLightedFlows) {
        for (HistoricActivityInstance historicActivityInstance : hisActInstList) {
            String id = historicActivityInstance.getActivityId();
            FlowNode node = flowNodeMap.get(id);
            boolean isParallel = false;
            if (node instanceof ParallelGateway || node instanceof InclusiveGateway) {
                isParallel = true;
            }
            List<SequenceFlow> allOutgoingFlows = new ArrayList<>();
            allOutgoingFlows.addAll(node.getOutgoingFlows());
            allOutgoingFlows.addAll(getBoundaryEventOutgoingFlows(node));
            List<String> activityHighLightedFlowIds = getHighlightedFlows(allOutgoingFlows, hisActInstList,
                    isParallel);
            highLightedFlows.addAll(activityHighLightedFlowIds);
        }
    }

    /**
     * 这是基于activiti5.x的写法翻译而来，因为flowable从6.X开始删掉了PVM包下面的东西，因此里面的API都尽量替换为flowable中相同或者相似功能的新API
     *
     * @param flowNodeMap 节点map
     * @param hisActInstList 流程中走过的节点
     * @param highLightedFlows 结果
     */
    @SuppressWarnings("unused")
    private void getHighlightedFlows(Map<String, FlowNode> flowNodeMap,
                                     LinkedList<HistoricActivityInstance> hisActInstList, List<String> highLightedFlows) {

        List<FlowNode> startEventActList = new ArrayList<>();
        for (FlowNode flowNode : flowNodeMap.values()) {
            if (flowNode instanceof StartEvent) {
                startEventActList.add(flowNode);
            }
        }


        HistoricActivityInstance firstHistActInst = hisActInstList.getFirst();
        String firstActType = firstHistActInst.getActivityType();
        if (firstActType != null && !firstActType.toLowerCase().contains("startevent")) {
            SequenceFlow startFlow = getStartFlow(startEventActList, firstHistActInst);
            if (startFlow != null) {
                highLightedFlows.add(startFlow.getId());
            }
        }

        while (!hisActInstList.isEmpty()) {
            HistoricActivityInstance histActInst = hisActInstList.removeFirst();
            FlowNode flowNode = flowNodeMap.get(histActInst.getActivityId());
            if (flowNode != null) {
                boolean isParallel = false;
                String type = histActInst.getActivityType();
                if ("parallelGateway".equals(type) || "inclusiveGateway".equals(type)) {
                    isParallel = true;
                } else if ("subProcess".equals(histActInst.getActivityType())) {
                    Map<String, FlowNode> map = getFlowNodes(((SubProcess) flowNode).getFlowElements());
                    getHighlightedFlows(map, hisActInstList, highLightedFlows);
                }

                List<SequenceFlow> allOutgoingFlows = new ArrayList<>();
                allOutgoingFlows.addAll(flowNode.getOutgoingFlows());
                allOutgoingFlows.addAll(getBoundaryEventOutgoingFlows(flowNode));
                List<String> activityHighLightedFlowIds = getHighlightedFlows(allOutgoingFlows, hisActInstList,
                        isParallel);
                highLightedFlows.addAll(activityHighLightedFlowIds);
            }
        }
    }

    private Map<String, FlowNode> getFlowNodes(Collection<FlowElement> elements) {
        return elements.stream().filter(element -> element instanceof FlowNode).map(e -> (FlowNode) e).collect(Collectors.toMap(BaseElement::getId, r -> r));
    }

    private List<SequenceFlow> getBoundaryEventOutgoingFlows(FlowNode flowNode) {
        List<SequenceFlow> boundaryFlows = new ArrayList<>();
        if (flowNode instanceof Activity) {
            List<BoundaryEvent> boundaryEvents = ((Activity) flowNode).getBoundaryEvents();
            for (BoundaryEvent boundaryEvent : boundaryEvents) {
                boundaryFlows.addAll(boundaryEvent.getOutgoingFlows());
            }
        }
        return boundaryFlows;
    }

    private List<String> getHighlightedFlows(List<SequenceFlow> flows,
                                             LinkedList<HistoricActivityInstance> hisActInstList, boolean isParallel) {

        List<String> highLightedFlowIds = new ArrayList<>();

        SequenceFlow earliestFlow = null;
        HistoricActivityInstance earliestHisActInst = null;

        for (SequenceFlow flow : flows) {

            String destActId = flow.getTargetRef();
            HistoricActivityInstance destHisActInst = findHisActInst(hisActInstList, destActId);
            if (destHisActInst != null) {
                if (isParallel) {
                    highLightedFlowIds.add(flow.getId());
                } else if (earliestHisActInst == null
                        || (earliestHisActInst.getId().compareTo(destHisActInst.getId()) > 0)) {
                    earliestFlow = flow;
                    earliestHisActInst = destHisActInst;
                }
            }
        }

        if ((!isParallel) && earliestFlow != null) {
            highLightedFlowIds.add(earliestFlow.getId());
        }

        return highLightedFlowIds;
    }

    private HistoricActivityInstance findHisActInst(LinkedList<HistoricActivityInstance> hisActInstList, String
            actId) {
        for (HistoricActivityInstance hisActInst : hisActInstList) {
            if (hisActInst.getActivityId().equals(actId)) {
                return hisActInst;
            }
        }
        return null;
    }

    private SequenceFlow getStartFlow(List<FlowNode> startEventActList,
                                      HistoricActivityInstance firstActInst) {
        for (FlowNode element : startEventActList) {
            for (SequenceFlow flow : element.getOutgoingFlows()) {
                if (flow.getTargetRef().equals(firstActInst.getActivityId())) {
                    return flow;
                }
            }
        }
        return null;
    }


}
