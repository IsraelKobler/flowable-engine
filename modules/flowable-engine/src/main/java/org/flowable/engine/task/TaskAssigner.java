package org.flowable.engine.task;

import org.flowable.engine.delegate.DelegateExecution;

import java.util.*;

import org.flowable.engine.*;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;

public class TaskAssigner implements JavaDelegate{
	
    public void execute(DelegateExecution execution) {
   	
	    System.out.println("inside TaskAssigner service class:");
	    
	    // get list of all the groups
	    IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
	    List<Group> userGroups = identityService.createGroupQuery().list();
		//System.out.println("group size "+userGroups.size());

		for (int g=0; g<userGroups.size(); g++) {
			String currentGrouodId = userGroups.get(g).getId();

			HashMap<User, Integer> userTaskCount = new HashMap<User, Integer>();

			//  find all group candidate tasks for buyer group
			TaskService taskService = Context.getProcessEngineConfiguration().getTaskService();


			List<Task> groupTasks = taskService.createTaskQuery().taskCandidateGroup(currentGrouodId).list();
			//System.out.println(currentGrouodId + " Group have " + groupTasks.size() + " tasks:");

			if (groupTasks.size() > 0) {

//				for (int i = 0; i < groupTasks.size(); i++) {
//					System.out.println((i + 1) + ") " + groupTasks.get(i).getName());
//				}

				// find all users for buyer group
				//int totalAssignedTasks = 0;
				identityService = Context.getProcessEngineConfiguration().getIdentityService();
				List<User> users = identityService.createUserQuery().memberOfGroup(currentGrouodId).list();
				//System.out.println(currentGrouodId + " Group have " + users.size() + " users:");

				if (users.size() > 0) {

					if (users.size() == 1) {
						for (Task task : groupTasks) {
							task.setAssignee(users.get(0).getId());
						}
					} else {

						for (int i = 0; i < users.size(); i++) {
							String userId = users.get(i).getId();
							//System.out.println((i + 1) + ") " + userId);
							int numOfUserTasks = (int) taskService.createTaskQuery().taskAssignee(userId).count();
							userTaskCount.put(users.get(i), numOfUserTasks);
						}

						userTaskCount = sortByValue(userTaskCount);
						//System.out.println("Total task unassigned " + groupTasks.size());

						if (userTaskCount.size() > 0) {

							int taskAssined = 0;
							while (taskAssined < groupTasks.size()) {
								//printUsers(userTaskCount);
								Map.Entry<User, Integer> entry = userTaskCount.entrySet().iterator().next();
								Task task = groupTasks.get(taskAssined);
								task.setAssignee(entry.getKey().getId());
								taskService.saveTask(task);
								System.out.println("Assigned task "+task.getId() + " - " +task.getName()+ " To - "+entry.getKey().getEmail());
								entry.setValue(entry.getValue() + 1);
								userTaskCount = sortByValue(userTaskCount);
								taskAssined++;
							}
						}
					}

				}
			}
		}
	    	    	    	
    }

	private void printUsers(Map<User, Integer> userTaskCount) {
		for (Map.Entry<User, Integer> entry : userTaskCount.entrySet()) {
			System.out.println("User " + entry.getKey().getEmail() + " has " + entry.getValue() + " Tasks");
		}
	}

	private HashMap<User, Integer> sortByValue(HashMap<User, Integer> hm)
	{
		// Create a list from elements of HashMap
		List<Map.Entry<User, Integer> > list =
				new LinkedList<>(hm.entrySet());

		// Sort the list
		Collections.sort(list, new Comparator<Map.Entry<User, Integer> >() {
			public int compare(Map.Entry<User, Integer> o1,
							   Map.Entry<User, Integer> o2)
			{
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// put data from sorted list to hashmap
		HashMap<User, Integer> temp = new LinkedHashMap<>();
		for (Map.Entry<User, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}
}
