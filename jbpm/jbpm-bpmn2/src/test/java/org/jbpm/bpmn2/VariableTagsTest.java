/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.bpmn2;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpmn2.objects.TestWorkItemHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.kogito.process.VariableViolationException;

public class VariableTagsTest extends JbpmBpmn2TestCase {

    private KieSession ksession2;

    @AfterEach
    @Override
    public void disposeSession() {
        super.disposeSession();
        if (ksession2 != null) {
            ksession2.dispose();
            ksession2 = null;
        }
    }

    @Test
    public void testProcessWithMissingRequiredVariable() throws Exception {
        KieBase kbase = createKnowledgeBase("variable-tags/approval-with-required-variable-tags.bpmn2");
        KieSession ksession = createKnowledgeSession(kbase);
        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        
        assertThrows(VariableViolationException.class, () -> ksession.startProcess("approvals"));
        
        ksession.dispose();
    }
    
    @Test
    public void testProcessWithRequiredVariable() throws Exception {
        KieBase kbase = createKnowledgeBase("variable-tags/approval-with-required-variable-tags.bpmn2");
        KieSession ksession = createKnowledgeSession(kbase);
        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        
        ProcessInstance processInstance = ksession.startProcess("approvals", parameters);
        assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
        ksession = restoreSession(ksession, true);
        WorkItem workItem = workItemHandler.getWorkItem();
        assertNotNull(workItem);
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
        
        workItem = workItemHandler.getWorkItem();
        assertNotNull(workItem);        
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
        
        assertProcessInstanceFinished(processInstance, ksession);
        ksession.dispose();
    }
    
    @Test
    public void testProcessWithReadonlyVariable() throws Exception {
        KieBase kbase = createKnowledgeBase("variable-tags/approval-with-readonly-variable-tags.bpmn2");
        KieSession ksession = createKnowledgeSession(kbase);
        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        
        ProcessInstance processInstance = ksession.startProcess("approvals", parameters);
        assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);        
        WorkItem workItem = workItemHandler.getWorkItem();
        assertNotNull(workItem);
                
        assertThrows(VariableViolationException.class, () -> ksession.getWorkItemManager().completeWorkItem(workItem.getId(), Collections.singletonMap("ActorId", "john")));
        
        ksession.abortProcessInstance(processInstance.getId());
        
        assertProcessInstanceFinished(processInstance, ksession);
        ksession.dispose();
    }
    
    @Test
    public void testProcessWithCustomVariableTag() throws Exception {
        KieBase kbase = createKnowledgeBase("variable-tags/approval-with-custom-variable-tags.bpmn2");
        KieSession ksession = createKnowledgeSession(kbase);
        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", workItemHandler);
        ksession.addEventListener(new DefaultProcessEventListener() {

            @Override
            public void beforeVariableChanged(ProcessVariableChangedEvent event) {
                if (event.hasTag("onlyAdmin")) {
                    throw new VariableViolationException(event.getProcessInstance().getId(), event.getVariableId(), "Variable can only be set by admins");
                }
            }
            
        });
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        
        assertThrows(VariableViolationException.class, () -> ksession.startProcess("approvals", parameters));
        
        ksession.dispose();
    }
}
