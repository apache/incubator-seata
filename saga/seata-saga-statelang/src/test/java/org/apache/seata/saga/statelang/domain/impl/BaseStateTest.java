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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BaseStateTest {
    private static class TestBaseState extends BaseState {}

    @Test
    public void testBaseStateAllProperties() {
        BaseState baseState = new TestBaseState();
        StateMachine mockStateMachine = new MockStateMachine();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("timeout", 3000);
        extensions.put("async", false);
        StateType testType = StateType.SERVICE_TASK;

        baseState.setName("orderCreateState");
        baseState.setComment("orderCreateStatus");
        baseState.setNext("orderPayState");
        baseState.setExtensions(extensions);
        baseState.setStateMachine(mockStateMachine);
        baseState.setType(testType);

        assertEquals("orderCreateState", baseState.getName(), "name can not be matched");
        assertEquals("orderCreateStatus", baseState.getComment(), "comment can not be matched");
        assertEquals("orderPayState", baseState.getNext(), "next can not be matched");
        assertSame(extensions, baseState.getExtensions(), "extensions can not be matched");
        assertEquals(3000, baseState.getExtensions().get("timeout"), "extension timeout can not be matched");
        assertSame(mockStateMachine, baseState.getStateMachine(), "stateMachine can not be matched");
        assertEquals(testType, baseState.getType(), "type can not be matched");
        assertEquals(StateType.SERVICE_TASK, baseState.getType(), "type should be SERVICE_TASK");
    }

    @Test
    public void testBaseStateNullProperties() {
        BaseState baseState = new TestBaseState();

        baseState.setName(null);
        baseState.setComment(null);
        baseState.setNext(null);
        baseState.setExtensions(null);
        baseState.setStateMachine(null);
        baseState.setType(null);

        Assertions.assertNull(baseState.getName(), "name should be null");
        Assertions.assertNull(baseState.getComment(), "comment should be null");
        Assertions.assertNull(baseState.getNext(), "next should be null");
        Assertions.assertNull(baseState.getExtensions(), "extensions should be null");
        Assertions.assertNull(baseState.getStateMachine(), "stateMachine should be null");
        Assertions.assertNull(baseState.getType(), "type should be null");
    }

    private static class MockStateMachine implements StateMachine {
        private String name;
        private String id;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getComment() {
            return "mockComment";
        }

        @Override
        public String getStartState() {
            return "mockStartState";
        }

        @Override
        public void setStartState(String startState) {}

        @Override
        public String getVersion() {
            return "1.0";
        }

        @Override
        public void setVersion(String version) {}

        @Override
        public Map<String, org.apache.seata.saga.statelang.domain.State> getStates() {
            return new HashMap<>();
        }

        @Override
        public org.apache.seata.saga.statelang.domain.State getState(String name) {
            return null;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getTenantId() {
            return "mockTenantId";
        }

        @Override
        public void setTenantId(String tenantId) {}

        @Override
        public String getAppName() {
            return "mockAppName";
        }

        @Override
        public String getType() {
            return "STATE_LANG";
        }

        @Override
        public Status getStatus() {
            return Status.AC;
        }

        @Override
        public org.apache.seata.saga.statelang.domain.RecoverStrategy getRecoverStrategy() {
            return org.apache.seata.saga.statelang.domain.RecoverStrategy.Compensate;
        }

        @Override
        public void setRecoverStrategy(org.apache.seata.saga.statelang.domain.RecoverStrategy recoverStrategy) {}

        @Override
        public String getContent() {
            return "mockContent";
        }

        @Override
        public void setContent(String content) {}

        @Override
        public boolean isPersist() {
            return true;
        }

        @Override
        public java.util.Date getGmtCreate() {
            return new java.util.Date();
        }

        @Override
        public void setGmtCreate(java.util.Date gmtCreate) {}

        @Override
        public Boolean isRetryPersistModeUpdate() {
            return true;
        }

        @Override
        public Boolean isCompensatePersistModeUpdate() {
            return false;
        }
    }
}
