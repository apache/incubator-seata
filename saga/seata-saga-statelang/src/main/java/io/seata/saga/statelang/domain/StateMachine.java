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
 * @author lorne.cl
 */
public interface StateMachine {

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

        Status(String statusString){
            this.statusString = statusString;
        }

        public String getStatusString() {
            return statusString;
        }
    }

    void setId(String id);

    void setStartState(String startState);

    /**
     * name
     * @return
     */
    String getName();

    /**
     * comment
     * @return
     */
    String getComment();

    /**
     * start state name
     * @return
     */
    String getStartState();

    /**
     * version
     * @return
     */
    String getVersion();

    /**
     * set version
     * @param version
     */
    void setVersion(String version);

    /**
     * states
     * @return
     */
    Map<String/** 状态机名称 **/, State> getStates();

    /**
     * get state
     * @param name
     * @return
     */
    State getState(String name);

    /**
     * get id
     * @return
     */
    String getId();

    /**
     * get tenantId
     * @return
     */
    String getTenantId();

    /**
     * set tenantId
     * @param tenantId
     */
    void setTenantId(String tenantId);

    /**
     * app name
     * @return
     */
    String getAppName();

    /**
     * type, there is only one type: SSL(SEATA state language)
     * @return
     */
    String getType();

    /**
     * statue（Active|Inactive）
     * @return
     */
    Status getStatus();

    /**
     * recover strategy: prefer compensation or forward when error occurred
     * @return
     */
    String getRecoverStrategy();

    /**
     * set RecoverStrategy
     * @param recoverStrategy
     */
    void setRecoverStrategy(String recoverStrategy);

    /**
     * Is it persist execution log to storage?, default true
     * @return
     */
    boolean isPersist();

    /**
     * State language text
     * @return
     */
    String getContent();

    void setContent(String content);

    /**
     * get create time
     * @return
     */
    Date getGmtCreate();

    /**
     * set create time
     * @param date
     */
    void setGmtCreate(Date date);
}