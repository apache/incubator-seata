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
package io.seata.saga.statelang.domain;

import java.util.Date;
import java.util.Map;

/**
 * StateMachine
 *
 * @author lorne.cl
 */
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

    enum Status {
        /**
         * Active
         */
        AC("Active"),
        /**
         * Inactive
         */
        IN("Inactive");

        private String statusString;

        Status(String statusString) {
            this.statusString = statusString;
        }

        public String getStatusString() {
            return statusString;
        }
    }
}