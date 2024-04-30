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
package io.seata.saga.statelang.domain;

import java.util.Date;
import java.util.Map;

/**
 * The interface State machine.
 */
@Deprecated
public interface StateMachine {

    /**
     * name
     *
     * @return the state machine name
     */
    String getName();

    /**
     * comment
     *
     * @return the state machine comment
     */
    String getComment();

    /**
     * start state name
     *
     * @return the start state name
     */
    String getStartState();

    /**
     * Sets start state.
     *
     * @param startState the start state
     */
    void setStartState(String startState);

    /**
     * version
     *
     * @return the state machine version
     */
    String getVersion();

    /**
     * set version
     *
     * @param version the state machine version
     */
    void setVersion(String version);

    /**
     * states
     *
     * @return the state machine key: the state machine name,value: the state machine
     */
    Map<String/** state machine name **/, State> getStates();

    /**
     * get state
     *
     * @param name the state machine name
     * @return the state machine
     */
    State getState(String name);

    /**
     * get id
     *
     * @return the state machine id
     */
    String getId();

    /**
     * Sets id.
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * get tenantId
     *
     * @return the tenant id
     */
    String getTenantId();

    /**
     * set tenantId
     *
     * @param tenantId the tenant id
     */
    void setTenantId(String tenantId);

    /**
     * app name
     *
     * @return the app name
     */
    String getAppName();

    /**
     * type, there is only one type: SSL(SEATA state language)
     *
     * @return the state type
     */
    String getType();

    /**
     * statue (Active|Inactive)
     *
     * @return the state machine status
     */
    Status getStatus();

    /**
     * recover strategy: prefer compensation or forward when error occurred
     *
     * @return the recover strategy
     */
    RecoverStrategy getRecoverStrategy();

    /**
     * set RecoverStrategy
     *
     * @param recoverStrategy the recover strategy
     */
    void setRecoverStrategy(RecoverStrategy recoverStrategy);

    /**
     * Is it persist execution log to storage?, default true
     *
     * @return is persist
     */
    boolean isPersist();

    /**
     * Is update last retry execution log, default append new
     *
     * @return the boolean
     */
    Boolean isRetryPersistModeUpdate();

    /**
     * Is update last compensate execution log, default append new
     *
     * @return the boolean
     */
    Boolean isCompensatePersistModeUpdate();

    /**
     * State language text
     *
     * @return the state language text
     */
    String getContent();

    /**
     * Sets content.
     *
     * @param content the content
     */
    void setContent(String content);

    /**
     * get create time
     *
     * @return the create gmt
     */
    Date getGmtCreate();

    /**
     * set create time
     *
     * @param date the create gmt
     */
    void setGmtCreate(Date date);

    /**
     * The enum Status.
     */
    enum Status {
        /**
         * Active
         */
        AC("Active"),
        /**
         * Inactive
         */
        IN("Inactive");

        private final String statusString;

        Status(String statusString) {
            this.statusString = statusString;
        }

        /**
         * Gets status string.
         *
         * @return the status string
         */
        public String getStatusString() {
            return statusString;
        }

        /**
         * Wrap status.
         *
         * @param target the target
         * @return the status
         */
        public static Status wrap(org.apache.seata.saga.statelang.domain.StateMachine.Status target) {
            if (target == null) {
                return null;
            }
            switch (target) {
                case AC:
                    return AC;
                case IN:
                    return IN;
                default:
                    throw new IllegalArgumentException("Cannot convert " + target.name());
            }
        }

        /**
         * Unwrap org . apache . seata . saga . statelang . domain . state machine . status.
         *
         * @return the org . apache . seata . saga . statelang . domain . state machine . status
         */
        public org.apache.seata.saga.statelang.domain.StateMachine.Status unwrap() {
            switch (this) {
                case AC:
                    return org.apache.seata.saga.statelang.domain.StateMachine.Status.AC;
                case IN:
                    return org.apache.seata.saga.statelang.domain.StateMachine.Status.IN;
                default:
                    throw new IllegalArgumentException("Cannot convert " + this.name());
            }
        }
    }
}
