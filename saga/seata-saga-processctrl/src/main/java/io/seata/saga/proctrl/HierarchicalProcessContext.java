/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.proctrl;

import java.util.Map;

/**
 * Hierarchical process context
 *
 * @author lorne.cl
 */
public interface HierarchicalProcessContext extends ProcessContext {

    /**
     * Gets get variable locally.
     *
     * @param name the name
     * @return the get variable locally
     */
    Object getVariableLocally(String name);

    /**
     * Sets set variable locally.
     *
     * @param name  the name
     * @param value the value
     */
    void setVariableLocally(String name, Object value);

    /**
     * Gets get variables locally.
     *
     * @return the get variables locally
     */
    Map<String, Object> getVariablesLocally();

    /**
     * Sets set variables locally.
     *
     * @param variables the variables
     */
    void setVariablesLocally(Map<String, Object> variables);

    /**
     * Has variable local boolean.
     *
     * @param name the name
     * @return the boolean
     */
    boolean hasVariableLocal(String name);

    /**
     * Remove variable locally.
     *
     * @param name the name
     */
    void removeVariableLocally(String name);

    /**
     * Clear locally.
     */
    void clearLocally();
}