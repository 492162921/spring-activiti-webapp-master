package org.activiti.web.simple.webapp.service.impl;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.diagram.ProcessDiagramGenerator;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.web.simple.webapp.service.ActivitiWorkFlowService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
  
  
/** 
 * ���̲���������<br> 
 * �˺�������Ҫ��������ͨ�������ء�ת�졢��ֹ������Ⱥ��Ĳ���<br> 
 *  
 *  
 */  
@Service("activitiWorkFlowServiceImpl")
public class  ActivitiWorkFlowServiceImpl implements ActivitiWorkFlowService{  
	
	@Autowired
	private IdentityService identityService;
	
	@Autowired
	private RepositoryService repositoryService;  
  
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private HistoryService historyService;


	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#backProcess(java.lang.String, java.lang.String, java.util.Map)
	 */  
    public void backProcess(String taskId, String activityId,  
            Map<String, Object> variables) throws Exception {  
        if (StringUtils.isEmpty(activityId)) {  
            throw new Exception("����Ŀ��ڵ�IDΪ�գ�");  
        }  
        // �������в�������ڵ㣬ͬʱ����  
        List<Task> taskList = findTaskListByKey(findProcessInstanceByTaskId(  
                taskId).getId(), findTaskById(taskId).getTaskDefinitionKey());  
        for (Task task : taskList) {  
            commitProcess(task.getId(), variables, activityId);  
        }  
    }


	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#callBackProcess(java.lang.String, java.lang.String)
	 */  
    public void callBackProcess(String taskId, String activityId)  
            throws Exception {  
        if (StringUtils.isEmpty(activityId)) {  
            throw new Exception("Ŀ��ڵ�IDΪ�գ�");  
        }  
  
        // �������в�������ڵ㣬ͬʱȡ��  
        List<Task> taskList = findTaskListByKey(findProcessInstanceByTaskId(  
                taskId).getId(), findTaskById(taskId).getTaskDefinitionKey());  
        for (Task task : taskList) {  
            commitProcess(task.getId(), null, activityId);  
        }  
    }


	/** 
     * ���ָ����ڵ����� 
     *  
     * @param activityImpl 
     *            ��ڵ� 
     * @return �ڵ����򼯺� 
     */  
    private List<PvmTransition> clearTransition(ActivityImpl activityImpl) {  
        // �洢��ǰ�ڵ�����������ʱ����  
        List<PvmTransition> oriPvmTransitionList = new ArrayList<PvmTransition>();  
        // ��ȡ��ǰ�ڵ��������򣬴洢����ʱ������Ȼ�����  
        List<PvmTransition> pvmTransitionList = activityImpl  
                .getOutgoingTransitions();  
        for (PvmTransition pvmTransition : pvmTransitionList) {  
            oriPvmTransitionList.add(pvmTransition);  
        }  
        pvmTransitionList.clear();  
  
        return oriPvmTransitionList;  
    }


	/** 
     * @param taskId 
     *            ��ǰ����ID 
     * @param variables 
     *            ���̱��� 
     * @param activityId 
     *            ����ת��ִ������ڵ�ID<br> 
     *            �˲���Ϊ�գ�Ĭ��Ϊ�ύ���� 
     * @throws Exception 
     */  
    private void commitProcess(String taskId, Map<String, Object> variables,  
            String activityId) throws Exception {  
        if (variables == null) {  
            variables = new HashMap<String, Object>();  
        }  
        // ��ת�ڵ�Ϊ�գ�Ĭ���ύ����  
        if (StringUtils.isEmpty(activityId)) {  
            taskService.complete(taskId, variables);  
        } else {// ����ת�����  
            turnTransition(taskId, activityId, variables);  
        }  
    }


	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#endProcess(java.lang.String)
	 */  
    public void endProcess(String taskId) throws Exception {  
        ActivityImpl endActivity = findActivitiImpl(taskId, "end");  
        commitProcess(taskId, null, endActivity.getId());  
    }


	/** 
     * �����������񼯺ϣ���ѯ���һ�ε���������ڵ� 
     *  
     * @param processInstance 
     *            ����ʵ�� 
     * @param tempList 
     *            �������񼯺� 
     * @return 
     */  
    private ActivityImpl filterNewestActivity(ProcessInstance processInstance,  
            List<ActivityImpl> tempList) {  
        while (tempList.size() > 0) {  
            ActivityImpl activity_1 = tempList.get(0);  
            HistoricActivityInstance activityInstance_1 = findHistoricUserTask(  
                    processInstance, activity_1.getId());  
            if (activityInstance_1 == null) {  
                tempList.remove(activity_1);  
                continue;  
            }  
  
            if (tempList.size() > 1) {  
                ActivityImpl activity_2 = tempList.get(1);  
                HistoricActivityInstance activityInstance_2 = findHistoricUserTask(  
                        processInstance, activity_2.getId());  
                if (activityInstance_2 == null) {  
                    tempList.remove(activity_2);  
                    continue;  
                }  
  
                if (activityInstance_1.getEndTime().before(  
                        activityInstance_2.getEndTime())) {  
                    tempList.remove(activity_1);  
                } else {  
                    tempList.remove(activity_2);  
                }  
            } else {  
                break;  
            }  
        }  
        if (tempList.size() > 0) {  
            return tempList.get(0);  
        }  
        return null;  
    }


	/** 
     * ��������ID�ͽڵ�ID��ȡ��ڵ� <br> 
     *  
     * @param taskId 
     *            ����ID 
     * @param activityId 
     *            ��ڵ�ID <br> 
     *            ���Ϊnull��""����Ĭ�ϲ�ѯ��ǰ��ڵ� <br> 
     *            ���Ϊ"end"�����ѯ�����ڵ� <br> 
     *  
     * @return 
     * @throws Exception 
     */  
    private ActivityImpl findActivitiImpl(String taskId, String activityId)  
            throws Exception {  
        // ȡ�����̶���  
        ProcessDefinitionEntity processDefinition = findProcessDefinitionEntityByTaskId(taskId);  
  
        // ��ȡ��ǰ��ڵ�ID  
        if (StringUtils.isEmpty(activityId)) {  
            activityId = findTaskById(taskId).getTaskDefinitionKey();  
        }  
  
        // �������̶��壬��ȡ������ʵ���Ľ����ڵ�  
        if (activityId.toUpperCase().equals("END")) {  
            for (ActivityImpl activityImpl : processDefinition.getActivities()) {  
                List<PvmTransition> pvmTransitionList = activityImpl  
                        .getOutgoingTransitions();  
                if (pvmTransitionList.isEmpty()) {  
                    return activityImpl;  
                }  
            }  
        }  
  
        // ���ݽڵ�ID����ȡ��Ӧ�Ļ�ڵ�  
        ActivityImpl activityImpl = ((ProcessDefinitionImpl) processDefinition)  
                .findActivity(activityId);  
  
        return activityImpl;  
    }


	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findBackAvtivity(java.lang.String)
	 */  
    public List<ActivityImpl> findBackAvtivity(String taskId) throws Exception {  
        List<ActivityImpl> rtnList =  iteratorBackActivity(taskId, findActivitiImpl(taskId,  
                    null), new ArrayList<ActivityImpl>(),  
                    new ArrayList<ActivityImpl>());  
        return reverList(rtnList);  
    }

	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findHistoricUserTask(org.activiti.engine.runtime.ProcessInstance, java.lang.String)
	 */  
    public HistoricActivityInstance findHistoricUserTask(  
            ProcessInstance processInstance, String activityId) {  
        HistoricActivityInstance rtnVal = null;  
        // ��ѯ��ǰ����ʵ��������������ʷ�ڵ�  
        List<HistoricActivityInstance> historicActivityInstances = historyService  
                .createHistoricActivityInstanceQuery().activityType("userTask")  
                .processInstanceId(processInstance.getId()).activityId(  
                        activityId).finished()  
                .orderByHistoricActivityInstanceEndTime().desc().list();  
        if (historicActivityInstances.size() > 0) {  
            rtnVal = historicActivityInstances.get(0);  
        }  
  
        return rtnVal;  
    }  
  
	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findParallelGatewayId(org.activiti.engine.impl.pvm.process.ActivityImpl)
	 */  
    public String findParallelGatewayId(ActivityImpl activityImpl) {  
        List<PvmTransition> incomingTransitions = activityImpl  
                .getOutgoingTransitions();  
        for (PvmTransition pvmTransition : incomingTransitions) {  
            TransitionImpl transitionImpl = (TransitionImpl) pvmTransition;  
            activityImpl = transitionImpl.getDestination();  
            String type = (String) activityImpl.getProperty("type");  
            if ("parallelGateway".equals(type)) {// ����·��  
                String gatewayId = activityImpl.getId();  
                String gatewayType = gatewayId.substring(gatewayId  
                        .lastIndexOf("_") + 1);  
                if ("END".equals(gatewayType.toUpperCase())) {  
                    return gatewayId.substring(0, gatewayId.lastIndexOf("_"))  
                            + "_start";  
                }  
            }  
        }  
        return null;  
    }  
  
	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findProcessDefinitionEntityByTaskId(java.lang.String)
	 */  
    public ProcessDefinitionEntity findProcessDefinitionEntityByTaskId(  
            String taskId) throws Exception {  
        // ȡ�����̶���  
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)  
                .getDeployedProcessDefinition(findTaskById(taskId)  
                        .getProcessDefinitionId());  
  
        if (processDefinition == null) {  
            throw new Exception("���̶���δ�ҵ�!");  
        }  
  
        return processDefinition;  
    }  
  
	/* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findProcessInstanceByTaskId(java.lang.String)
	 */  
    public ProcessInstance findProcessInstanceByTaskId(String taskId)  
            throws Exception {  
        // �ҵ�����ʵ��  
        ProcessInstance processInstance = runtimeService  
                .createProcessInstanceQuery().processInstanceId(  
                        findTaskById(taskId).getProcessInstanceId())  
                .singleResult();  
        if (processInstance == null) {   
            throw new Exception("����ʵ��δ�ҵ�!");  
        }  
        return processInstance;  
    }  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findTaskById(java.lang.String)
	 */  
    public TaskEntity findTaskById(String taskId) throws Exception {  
        TaskEntity task = (TaskEntity) taskService.createTaskQuery().taskId(  
                taskId).singleResult();  
        if (task == null) {  
            throw new Exception("����ʵ��δ�ҵ�!");  
        }  
        return task;  
    }  
  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#findTaskListByKey(java.lang.String, java.lang.String)
	 */  
    public List<Task> findTaskListByKey(String processInstanceId, String key) {  
        return taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(key).list();  
    }  
  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#iteratorBackActivity(java.lang.String, org.activiti.engine.impl.pvm.process.ActivityImpl, java.util.List, java.util.List)
	 */  
    public List<ActivityImpl> iteratorBackActivity(String taskId,  
            ActivityImpl currActivity, List<ActivityImpl> rtnList,  
            List<ActivityImpl> tempList) throws Exception {  
        // ��ѯ���̶��壬�����������ṹ  
        ProcessInstance processInstance = findProcessInstanceByTaskId(taskId);  
  
        // ��ǰ�ڵ��������Դ  
        List<PvmTransition> incomingTransitions = currActivity  
                .getIncomingTransitions();  
        // ������֧�ڵ㼯�ϣ�userTask�ڵ������ϣ����������˼��ϣ���ѯ������֧��Ӧ��userTask�ڵ�  
        List<ActivityImpl> exclusiveGateways = new ArrayList<ActivityImpl>();  
        // ���нڵ㼯�ϣ�userTask�ڵ������ϣ����������˼��ϣ���ѯ���нڵ��Ӧ��userTask�ڵ�  
        List<ActivityImpl> parallelGateways = new ArrayList<ActivityImpl>();  
        // ������ǰ�ڵ���������·��  
        for (PvmTransition pvmTransition : incomingTransitions) {  
            TransitionImpl transitionImpl = (TransitionImpl) pvmTransition;  
            ActivityImpl activityImpl = transitionImpl.getSource();  
            String type = (String) activityImpl.getProperty("type");  
            /** 
             * ���нڵ�����Ҫ��<br> 
             * ����ɶԳ��֣���Ҫ��ֱ����ýڵ�IDΪ:XXX_start(��ʼ)��XXX_end(����) 
             */  
            if ("parallelGateway".equals(type)) {// ����·��  
                String gatewayId = activityImpl.getId();  
                String gatewayType = gatewayId.substring(gatewayId  
                        .lastIndexOf("_") + 1);  
                if ("START".equals(gatewayType.toUpperCase())) {// ������㣬ֹͣ�ݹ�  
                    return rtnList;  
                } else {// �����յ㣬��ʱ�洢�˽ڵ㣬����ѭ���������������ϣ���ѯ��Ӧ��userTask�ڵ�  
                    parallelGateways.add(activityImpl);  
                }  
            } else if ("startEvent".equals(type)) {// ��ʼ�ڵ㣬ֹͣ�ݹ�  
                return rtnList;  
            } else if ("userTask".equals(type)) {// �û�����  
                tempList.add(activityImpl);  
            } else if ("exclusiveGateway".equals(type)) {// ��֧·�ߣ���ʱ�洢�˽ڵ㣬����ѭ���������������ϣ���ѯ��Ӧ��userTask�ڵ�  
                currActivity = transitionImpl.getSource();  
                exclusiveGateways.add(currActivity);  
            }  
        }  
  
        /** 
         * ����������֧���ϣ���ѯ��Ӧ��userTask�ڵ� 
         */  
        for (ActivityImpl activityImpl : exclusiveGateways) {  
            iteratorBackActivity(taskId, activityImpl, rtnList, tempList);  
        }  
  
        /** 
         * �������м��ϣ���ѯ��Ӧ��userTask�ڵ� 
         */  
        for (ActivityImpl activityImpl : parallelGateways) {  
            iteratorBackActivity(taskId, activityImpl, rtnList, tempList);  
        }  
  
        /** 
         * ����ͬ��userTask���ϣ�������������Ľڵ� 
         */  
        currActivity = filterNewestActivity(processInstance, tempList);  
        if (currActivity != null) {  
            // ��ѯ��ǰ�ڵ�������Ƿ�Ϊ�����յ㣬����ȡ�������ID  
            String id = findParallelGatewayId(currActivity);  
            if (StringUtils.isEmpty(id)) {// �������IDΪ�գ��˽ڵ������ǲ����յ㣬���ϲ����������洢�˽ڵ�  
                rtnList.add(currActivity);  
            } else {// ���ݲ������ID��ѯ��ǰ�ڵ㣬Ȼ�������ѯ���Ӧ��userTask����ڵ�  
                currActivity = findActivitiImpl(taskId, id);  
            }  
  
            // ��ձ��ε�����ʱ����  
            tempList.clear();  
            // ִ���´ε���  
            iteratorBackActivity(taskId, currActivity, rtnList, tempList);  
        }  
        return rtnList;  
    }  
  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#restoreTransition(org.activiti.engine.impl.pvm.process.ActivityImpl, java.util.List)
	 */  
    public void restoreTransition(ActivityImpl activityImpl,  
            List<PvmTransition> oriPvmTransitionList) {  
        // �����������  
        List<PvmTransition> pvmTransitionList = activityImpl  
                .getOutgoingTransitions();  
        pvmTransitionList.clear();  
        // ��ԭ��ǰ����  
        for (PvmTransition pvmTransition : oriPvmTransitionList) {  
            pvmTransitionList.add(pvmTransition);  
        }  
    }  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#reverList(java.util.List)
	 */  
    public List<ActivityImpl> reverList(List<ActivityImpl> list) {  
        List<ActivityImpl> rtnList = new ArrayList<ActivityImpl>();  
        // ���ڵ��������ظ����ݣ��ų��ظ�  
        for (int i = list.size(); i > 0; i--) {  
            if (!rtnList.contains(list.get(i - 1)))  
                rtnList.add(list.get(i - 1));  
        }  
        return rtnList;  
    }  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#transferAssignee(java.lang.String, java.lang.String)
	 */  
    public void transferAssignee(String taskId, String userCode) {  
        taskService.setAssignee(taskId, userCode);  
    }  
  
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#turnTransition(java.lang.String, java.lang.String, java.util.Map)
	 */  
    public void turnTransition(String taskId, String activityId,  
            Map<String, Object> variables) throws Exception {  
        // ��ǰ�ڵ�  
        ActivityImpl currActivity = findActivitiImpl(taskId, null);  
        // ��յ�ǰ����  
        List<PvmTransition> oriPvmTransitionList = clearTransition(currActivity);  
  
        // ����������  
        TransitionImpl newTransition = currActivity.createOutgoingTransition();  
        // Ŀ��ڵ�  
        ActivityImpl pointActivity = findActivitiImpl(taskId, activityId);  
        // �����������Ŀ��ڵ�  
        newTransition.setDestination(pointActivity);  
  
        // ִ��ת������  
        taskService.complete(taskId, variables);  
        // ɾ��Ŀ��ڵ�������  
        pointActivity.getIncomingTransitions().remove(newTransition);  
  
        // ��ԭ��ǰ����  
        restoreTransition(currActivity, oriPvmTransitionList);  
    }  
    
    /* (non-Javadoc)
	 * @see org.activiti.web.simple.webapp.service.ActivitiWorkFlowService#getImageStream(java.lang.String)
	 */
    public InputStream getImageStream(String taskId) throws Exception{
    	ProcessDefinitionEntity pde = findProcessDefinitionEntityByTaskId(taskId);
    	InputStream imageStream = ProcessDiagramGenerator.generateDiagram(
    	        null,
                "png",
		        runtimeService.getActiveActivityIds(findProcessInstanceByTaskId(taskId).getId()));
    	return imageStream;
    }

    /**
     * ��֤��¼
     */
	public boolean login(String userid, String password) throws Exception {
		return identityService.checkPassword(userid, password);
	}

	/**
	 * ��ȡ�û���ϸ��Ϣ
	 */
	public User getUserInfo(String userid) {
		return identityService.createUserQuery().userId(userid).singleResult();
	}

	/**
	 * �����û�id��ѯ�û����ڵ���
	 */
	public List<Group> getUserOfGroup(String userid) {
		return identityService.createGroupQuery().groupMember(userid).list();
	}


	/**
	 * ����groupId��ѯ����ϸ��Ϣ
	 */
	public Group getGroupInfo(String groupId) {
		return identityService.createGroupQuery().groupId(groupId).singleResult();
	}

	/**
	 * �г����ڵ������û�
	 */
	public List<User> memberOfGroup(String groupId) {
		return identityService.createUserQuery().memberOfGroup(groupId).list();
	}
}