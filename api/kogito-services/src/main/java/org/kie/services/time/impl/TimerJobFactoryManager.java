/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.kie.services.time.impl;

import java.util.Collection;

import org.kie.services.time.InternalSchedulerService;
import org.kie.services.time.Job;
import org.kie.services.time.JobContext;
import org.kie.services.time.JobHandle;
import org.kie.services.time.Trigger;

public interface TimerJobFactoryManager {
    TimerJobInstance createTimerJobInstance(Job job,
                                            JobContext ctx,
                                            Trigger trigger,
                                            JobHandle handle,
                                            InternalSchedulerService scheduler);
    
    void addTimerJobInstance(TimerJobInstance instance);
    
    void removeTimerJobInstance(TimerJobInstance instance);

    Collection<TimerJobInstance> getTimerJobInstances();
}
