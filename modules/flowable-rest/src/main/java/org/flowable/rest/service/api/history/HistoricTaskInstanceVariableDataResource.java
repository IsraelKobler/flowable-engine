/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.service.api.history;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import java.util.*;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "History Task" }, description = "Manage History Task Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskInstanceVariableDataResource extends HistoricTaskInstanceBaseResource{

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Get the binary data for a historic task instance variable", tags = {"History" }, nickname = "getHistoricTaskInstanceVariableData",
            notes = "The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task instance was found and the requested variable data is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task instance was not found or the process instance does not have a variable with the given name or the variable does not have a binary stream available. Status message provides additional information.") })
    @GetMapping(value = "/history/historic-task-instances/{taskId}/variables/{variableName}/data")
    @ResponseBody
    public byte[] getVariableData(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            byte[] result = null;
            RestVariable variable = getVariableFromRequest(true, taskId, variableName, scope, request);
            if (RestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE.equals(variable.getType())) {
                result = (byte[]) variable.getValue();
                response.setContentType("application/octet-stream");

            } else if (RestResponseFactory.SERIALIZABLE_VARIABLE_TYPE.equals(variable.getType())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
                outputStream.writeObject(variable.getValue());
                outputStream.close();
                result = buffer.toByteArray();
                response.setContentType("application/x-java-serialized-object");

            } else {
                throw new FlowableObjectNotFoundException("The variable does not have a binary data stream.", null);
            }
            return result;

        } catch (IOException ioe) {
            // Re-throw IOException
            throw new FlowableException("Unexpected exception getting variable data", ioe);
        }
    }

    @ApiOperation(value = "List variables for a historic task", tags = {"Task Variables" }, nickname = "listHistoricTaskVariables")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the historic task was found and the requested variables are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested historic task was not found..")
    })
    @ApiImplicitParams(@ApiImplicitParam(name = "scope", dataType = "string", value = "Scope of variable to be returned. When local, only task-local variable value is returned. When global, only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.", paramType = "query"))
    @GetMapping(value = "/history/historic-task-instances/{taskId}/variables", produces = "application/json")
    public List<RestVariable> getVariables(@ApiParam(name = "taskId") @PathVariable String taskId, @ApiParam(hidden = true) @RequestParam(value = "scope", required = false) String scope, HttpServletRequest request) {

        // Check if it's a valid task to get the variables for
        HistoricTaskInstance task = getHistoricTaskInstanceFromRequest(taskId);

        Map<String, RestVariable> variableMap = new HashMap<>();


        RestVariableScope variableScope = RestVariable.getScopeFromString(scope);
        if (variableScope == null) {
            // Use both local and global variables
            addLocalVariables(task, variableMap);
            addGlobalVariables(task, variableMap);

        } else if (variableScope == RestVariableScope.GLOBAL) {
            addGlobalVariables(task, variableMap);

        } else if (variableScope == RestVariableScope.LOCAL) {
            addLocalVariables(task, variableMap);
        }

        // Get unique variables from map
        List<RestVariable> result = new ArrayList<>(variableMap.values());
        return result;
    }

    public RestVariable getVariableFromRequest(boolean includeBinary, String taskId, String variableName, String scope, HttpServletRequest request) {
        RestVariableScope variableScope = RestVariable.getScopeFromString(scope);
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery().taskId(taskId);

        if (variableScope != null) {
            if (variableScope == RestVariableScope.GLOBAL) {
                taskQuery.includeProcessVariables();
            } else {
                taskQuery.includeTaskLocalVariables();
            }
        } else {
            taskQuery.includeTaskLocalVariables().includeProcessVariables();
        }

        HistoricTaskInstance taskObject = taskQuery.singleResult();

        if (taskObject == null) {
            throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' could not be found.", HistoricTaskInstanceEntity.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryTaskInfoById(taskObject);
        }

        Object value = null;
        if (variableScope != null) {
            if (variableScope == RestVariableScope.GLOBAL) {
                value = taskObject.getProcessVariables().get(variableName);
            } else {
                value = taskObject.getTaskLocalVariables().get(variableName);
            }
        } else {
            // look for local task variables first
            if (taskObject.getTaskLocalVariables().containsKey(variableName)) {
                value = taskObject.getTaskLocalVariables().get(variableName);
            } else {
                value = taskObject.getProcessVariables().get(variableName);
            }
        }

        if (value == null) {
            throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' variable value for " + variableName + " could not be found.", VariableInstanceEntity.class);
        } else {
            return restResponseFactory.createRestVariable(variableName, value, null, taskId, RestResponseFactory.VARIABLE_HISTORY_TASK, includeBinary);
        }
    }

    private void addGlobalVariables(HistoricTaskInstance task, Map<String, RestVariable> variableMap) {
        if (task.getExecutionId() != null) {
            Map<String, Object> rawVariables = task.getProcessVariables();
            List<RestVariable> globalVariables = restResponseFactory.createRestVariables(rawVariables, task.getId(), RestResponseFactory.VARIABLE_HISTORY_TASK, RestVariableScope.GLOBAL);

            // Overlay global variables over local ones. In case they are present the values are not overridden,
            // since local variables get precedence over global ones at all times.
            for (RestVariable var : globalVariables) {
                if (!variableMap.containsKey(var.getName())) {
                    variableMap.put(var.getName(), var);
                }
            }
        }
    }

    private void addLocalVariables(HistoricTaskInstance task, Map<String, RestVariable> variableMap) {
        Map<String, Object> rawVariables = task.getTaskLocalVariables();
        List<RestVariable> localVariables = restResponseFactory.createRestVariables(rawVariables, task.getId(), RestResponseFactory.VARIABLE_HISTORY_TASK, RestVariableScope.LOCAL);

        for (RestVariable var : localVariables) {
            variableMap.put(var.getName(), var);
        }
    }
}
