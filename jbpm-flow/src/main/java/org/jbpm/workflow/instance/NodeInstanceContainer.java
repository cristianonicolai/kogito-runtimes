/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jbpm.workflow.instance;

import java.util.Collection;

import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;

/**
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public interface NodeInstanceContainer extends org.kie.api.runtime.process.NodeInstanceContainer {

    Collection<NodeInstance> getNodeInstances(boolean recursive);

    NodeInstance getFirstNodeInstance(long nodeId);

    NodeInstance getNodeInstance(Node node);

    void addNodeInstance(NodeInstance nodeInstance);

    void removeNodeInstance(NodeInstance nodeInstance);
    
    NodeContainer getNodeContainer();
    
    void nodeInstanceCompleted(NodeInstance nodeInstance, String outType);
    
    int getState();
    
    void setState(int state);
    
    int getLevelForNode(String uniqueID);
    
    void setCurrentLevel(int level);
    
    int getCurrentLevel();
    
    NodeInstance getNodeInstance(long nodeInstanceId, boolean recursive);

}
