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
 * Process Context
 *
 * @author jin.xie
 * @author lorne.cl
 */
public interface ProcessContext {

    String VAR_NAME_PROCESS_TYPE = "_ProcessType_";

    /**
     * Gets get variable.
     *
     * @param name the name
     * @return the get variable
     */
    Object getVariable(String name);

    /**
     * Sets set variable.
     *
     * @param name the name
     * @param value the value
     */
    void setVariable(String name, Object value);

    /**
     * Gets get variables.
     *
     * @return the get variables
     */
    Map<String, Object> getVariables();

    /**
     * Sets set variables.
     *
     * @param variables the variables
     */
    void setVariables(Map<String, Object> variables);

    /**
     * Remove variable.
     *
     * @param name the name
     */
    void removeVariable(String name);

    /**
     * Has variable boolean.
     *
     * @param name the name
     * @return the boolean
     */
    boolean hasVariable(String name);

    /**
     * Gets get instruction.
     *
     * @return the get instruction
     */
    Instruction getInstruction();

    /**
     * Gets get instruction.
     *
     * @param <T> the type parameter
     * @param clazz the clazz
     * @return the get instruction
     */
    <T extends Instruction> T getInstruction(Class<T> clazz);

    /**
     * Sets set instruction.
     *
     * @param instruction the instruction
     */
    void setInstruction(Instruction instruction);
}