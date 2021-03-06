/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.process.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.drools.core.time.TimeUtils;
import org.jbpm.process.core.timer.DateTimeUtils;
import org.jbpm.process.core.timer.Timer;
import org.jbpm.process.instance.LightProcessRuntime;
import org.jbpm.process.instance.LightProcessRuntimeContext;
import org.jbpm.process.instance.LightProcessRuntimeServiceProvider;
import org.jbpm.process.instance.ProcessRuntimeServiceProvider;
import org.jbpm.workflow.core.impl.WorkflowProcessImpl;
import org.jbpm.workflow.core.node.StartNode;
import org.kie.api.runtime.process.EventListener;
import org.kie.api.runtime.process.ProcessRuntime;
import org.kie.kogito.Model;
import org.kie.kogito.jobs.DurationExpirationTime;
import org.kie.kogito.jobs.ExactExpirationTime;
import org.kie.kogito.jobs.ExpirationTime;
import org.kie.kogito.jobs.ProcessJobDescription;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstances;
import org.kie.kogito.process.ProcessInstancesFactory;
import org.kie.kogito.process.Signal;

public abstract class AbstractProcess<T extends Model> implements Process<T> {

    protected ProcessInstancesFactory processInstancesFactory;

    protected MutableProcessInstances<T> instances;

    protected final ProcessRuntimeServiceProvider services;

    protected CompletionEventListener completionEventListener = new CompletionEventListener();
    
    protected boolean activated;
    protected List<String> startTimerInstances = new ArrayList<>();
    protected ProcessRuntime processRuntime;

    protected AbstractProcess() {
        this(new LightProcessRuntimeServiceProvider());
    }

    protected AbstractProcess(ProcessConfig config) {
        this(new ConfiguredProcessServices(config));
    }
    
    protected AbstractProcess(ProcessRuntimeServiceProvider services) {
        this.services = services;
        this.instances = new MapProcessInstances<>();   
        
    }
    
    @Override
    public String id() {
        return legacyProcess().getId();
    }

    @Override
    public T createModel() {
        return null;
    }    

    @Override
    public ProcessInstance<T> createInstance(Model m) {
        return createInstance((T) m);
    }

    @Override
    public ProcessInstance<T> createInstance(String businessKey, Model m) {
        return createInstance(businessKey, m);
    }

    @Override
    public ProcessInstances<T> instances() {
        return instances;
    }

    @Override
    public <S> void send(Signal<S> signal) {
        instances().values().forEach(pi -> pi.send(signal));
    }
    
    public Process<T> configure() {

        registerListeners();
        if (isProcessFactorySet()) {
            this.instances = (MutableProcessInstances<T>) processInstancesFactory.createProcessInstances(this);
        }
        //services.getWorkItemManager().registerWorkItemHandler(name, handlerConfig.forName(name)
        //services.getEventSupport().addEventListener(listener)
        

        return this;
    }

    protected void registerListeners() {
        
    }

    @Override
    public void activate() {

        configure();
        WorkflowProcessImpl p = (WorkflowProcessImpl) legacyProcess();
        List<StartNode> startNodes = p.getTimerStart();
        if (startNodes != null && !startNodes.isEmpty()) {
            this.processRuntime = createLegacyProcessRuntime();
            for (StartNode startNode : startNodes) {
                if (startNode != null && startNode.getTimer() != null) {

                    String timerId = processRuntime.getJobsService().scheduleProcessJob(ProcessJobDescription.of(configureTimerInstance(startNode.getTimer()), this));

                    startTimerInstances.add(timerId);
                }
            }
        }
        this.activated = true;
    }

    @Override
    public void deactivate() {

        for (String startTimerId : startTimerInstances) {
            this.processRuntime.getJobsService().cancelJob(startTimerId);
        }
        this.activated = false;
    }

    protected ExpirationTime configureTimerInstance(Timer timer) {
        long duration = -1;
        switch (timer.getTimeType()) {
            case Timer.TIME_CYCLE:
                // when using ISO date/time period is not set
                long[] repeatValues = DateTimeUtils.parseRepeatableDateTime(timer.getDelay());
                if (repeatValues.length == 3) {
                    int parsedReapedCount = (int) repeatValues[0];
                    if (parsedReapedCount > -1) {
                        parsedReapedCount = Integer.MAX_VALUE;
                    }
                    return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2]);
                } else {
                    long delay = repeatValues[0];
                    long period = -1;
                    try {
                        period = TimeUtils.parseTimeString(timer.getPeriod());

                    } catch (RuntimeException e) {
                        period = repeatValues[0];
                    }

                    return DurationExpirationTime.repeat(delay, period);
                }

            case Timer.TIME_DURATION:

                duration = DateTimeUtils.parseDuration(timer.getDelay());
                return DurationExpirationTime.repeat(duration);

            case Timer.TIME_DATE:

                return ExactExpirationTime.of(timer.getDate());

            default:
                throw new UnsupportedOperationException("Not supported timer definition");
        }

    }

    public abstract org.kie.api.definition.process.Process legacyProcess();

    protected ProcessRuntime createLegacyProcessRuntime() {        
        return new LightProcessRuntime(
                new LightProcessRuntimeContext(Collections.singletonList(legacyProcess())),
                services);
    }

    protected boolean isProcessFactorySet() {
        return processInstancesFactory != null;
    }    
    
    public void setProcessInstancesFactory(ProcessInstancesFactory processInstancesFactory) {
        this.processInstancesFactory = processInstancesFactory;
    }
    
    private class CompletionEventListener implements EventListener {
        
        @Override
        public void signalEvent(String type, Object event) {
            if (type.startsWith("processInstanceCompleted:")) {
                org.kie.api.runtime.process.ProcessInstance pi = (org.kie.api.runtime.process.ProcessInstance) event;
                if (!id().equals(pi.getProcessId()) && pi.getParentProcessInstanceId() != null) {
                    instances().findById(pi.getParentProcessInstanceId()).ifPresent(p -> p.send(Sig.of(type, event)));
                }
            }
        }
        
        @Override
        public String[] getEventTypes() {
            return new String[0];
        }
    }
}
