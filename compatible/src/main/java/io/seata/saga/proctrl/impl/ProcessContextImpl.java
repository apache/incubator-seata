/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.saga.proctrl.impl;

import java.util.Map;

import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.Instruction;

/**
 * The default process context implementation
 *
 */
@Deprecated
public class ProcessContextImpl implements HierarchicalProcessContext, ProcessContext {

    private final org.apache.seata.saga.proctrl.HierarchicalProcessContext actual;

    private ProcessContextImpl(org.apache.seata.saga.proctrl.HierarchicalProcessContext target) {
        this.actual = target;
    }

    @Override
    public Object getVariable(String name) {
        return actual.getVariable(name);
    }

    @Override
    public void setVariable(String name, Object value) {
        actual.setVariable(name, value);
    }

    @Override
    public Map<String, Object> getVariables() {
        return actual.getVariables();
    }

    @Override
    public void setVariables(final Map<String, Object> variables) {
        actual.setVariables(variables);
    }

    @Override
    public Object getVariableLocally(String name) {
        return actual.getVariableLocally(name);
    }

    @Override
    public void setVariableLocally(String name, Object value) {
        actual.setVariableLocally(name, value);
    }

    @Override
    public Map<String, Object> getVariablesLocally() {
        return actual.getVariablesLocally();
    }

    @Override
    public void setVariablesLocally(Map<String, Object> variables) {
        actual.setVariablesLocally(variables);
    }

    @Override
    public boolean hasVariable(String name) {
        return actual.hasVariable(name);
    }

    @Override
    public Instruction getInstruction() {
        return actual.getInstruction();
    }

    @Override
    public void setInstruction(Instruction instruction) {
        actual.setInstruction(instruction);
    }

    @Override
    public <T extends Instruction> T getInstruction(Class<T> clazz) {
        return actual.getInstruction(clazz);
    }

    @Override
    public boolean hasVariableLocal(String name) {
        return actual.hasVariableLocal(name);
    }

    @Override
    public Object removeVariable(String name) {
        return actual.removeVariable(name);
    }

    @Override
    public Object removeVariableLocally(String name) {
        return actual.removeVariableLocally(name);
    }

    @Override
    public void clearLocally() {
        actual.clearLocally();
    }

    public ProcessContext getParent() {
        return wrap((org.apache.seata.saga.proctrl.HierarchicalProcessContext)
                ((org.apache.seata.saga.proctrl.impl.ProcessContextImpl) actual).getParent());
    }

    public void setParent(ProcessContext parent) {
        ((org.apache.seata.saga.proctrl.impl.ProcessContextImpl) actual)
                .setParent(((ProcessContextImpl) parent).unwrap());
    }

    @Override
    public String toString() {
        return actual.toString();
    }

    public static ProcessContextImpl wrap(org.apache.seata.saga.proctrl.HierarchicalProcessContext target) {
        return new ProcessContextImpl(target);
    }

    public org.apache.seata.saga.proctrl.ProcessContext unwrap() {
        return actual;
    }
}
