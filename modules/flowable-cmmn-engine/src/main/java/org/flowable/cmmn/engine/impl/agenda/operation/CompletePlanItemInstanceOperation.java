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
package org.flowable.cmmn.engine.impl.agenda.operation;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CompletePlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {
    
    public CompletePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    protected String getNewState() {
        return PlanItemInstanceState.COMPLETED;
    }
    
    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.COMPLETE;
    }
    
    @Override
    protected boolean isEvaluateRepetitionRule() {
        return true;
    }

    @Override
    protected void internalExecute() {
        if (isStage(planItemInstanceEntity)) {
            // terminate any remaining child plan items (e.g. in enabled / available state), but don't complete them as it might lead
            // into wrong behavior resulting from it (e.g. triggering some follow-up actions on that completion event) and it will leave
            // such implicitly completed plan items in complete state although they were never explicitly completed
            exitChildPlanItemInstances();
        }

        planItemInstanceEntity.setEndedTime(getCurrentTime(commandContext));
        planItemInstanceEntity.setCompletedTime(planItemInstanceEntity.getEndedTime());
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceCompleted(planItemInstanceEntity);
    }
    
}
