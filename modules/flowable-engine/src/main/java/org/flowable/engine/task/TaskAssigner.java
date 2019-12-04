package org.flowable.engine.task;

import org.flowable.engine.delegate.DelegateExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.*;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;

public class TaskAssigner implements JavaDelegate{
	
    public void execute(DelegateExecution execution) {
   	
	    System.out.println("inside TaskAssigner service class:");
	    
	    
	    // get list of all the groups
	    IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
	    List<Group> userGroups = identityService.createGroupQuery().list();
	    
	    for (int g=0; g<userGroups.size(); g++)
	    {
	    	String currentGrouodId = userGroups.get(g).getId();

	    	Map<String, Integer> userTaskCount = new HashMap<String, Integer>();	    
	    	//  find all group candidate tasks for buyer group
	    	TaskService taskService = Context.getProcessEngineConfiguration().getTaskService();

	    
	    	List<Task> groupTasks = taskService.createTaskQuery().taskCandidateGroup(currentGrouodId).list();
	    	System.out.println(currentGrouodId + " Group have " + groupTasks.size() + " tasks:");	    	    		    	    	
	    	for (int i=0; i<groupTasks.size(); i++) {
	    		System.out.println((i+1) + ") " + groupTasks.get(i).getName());	    	    	      
	    	}
	    
	    	// find all users for buyer group
	    	int totalAssignedTasks = 0;
	    	identityService = Context.getProcessEngineConfiguration().getIdentityService();
	    	List<User> users = identityService.createUserQuery().memberOfGroup(currentGrouodId).list();
	    	System.out.println(currentGrouodId + " Group have " + users.size() + " users:");
	    	for (int i=0; i<users.size(); i++) {	
	    		String userId = users.get(i).getId();
	    		System.out.println((i+1) + ") " + userId);
	    		int numOfUserTasks = (int) taskService.createTaskQuery().taskAssignee(userId).count();
	   	      	userTaskCount.put(userId, numOfUserTasks);
	   	      	totalAssignedTasks = totalAssignedTasks + numOfUserTasks;
	    	}

	    	int averageNumOfTasks = 0;
	    	if (users.size() > 0)
	    		averageNumOfTasks = totalAssignedTasks / users.size();
	    	for (int i=0; i<groupTasks.size(); i++)
	    	{
	    		int j = 0;
	    		boolean foundUser = false;
	    	
	    		while (j<users.size() && !foundUser)
	    		{

	    			if (userTaskCount.get(users.get(j).getId()) >= averageNumOfTasks)
	    			{
	    				groupTasks.get(i).setAssignee(users.get(j).getId());
	    				totalAssignedTasks++;
	    				averageNumOfTasks = totalAssignedTasks / users.size();
	    				foundUser = true;
	    			}
	    			else
	    				j++;
	    		}
	    	}
	    
	    }
	    	    	    	
    }
}
