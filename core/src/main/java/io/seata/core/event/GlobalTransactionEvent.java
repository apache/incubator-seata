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
package io.seata.core.event;

/**
 * Event data for global transaction.
 *
 * @author zhengyangyong
 */
public class GlobalTransactionEvent implements Event {
    /**
     * The constant ROLE_TC.
     */
    public static final String ROLE_TC = "tc";

    /**
     * The constant ROLE_TM.
     */
    public static final String ROLE_TM = "tm";

    /**
     * The constant ROLE_RM.
     */
    public static final String ROLE_RM = "rm";

    /**
     * Transaction Id
     */
    private long id;

    /**
     * Source Role
     */
    private final String role;

    /**
     * Transaction Name
     */
    private final String name;

    /**
     * business applicationId
     */
    private String applicationId;

    /**
     * Transaction Service Group
     */
    private String group;

    /**
     * Transaction Begin Time
     */
    private final Long beginTime;

    /**
     * Transaction End Time (If Transaction do not committed or rollbacked, null)
     */
    private final Long endTime;

    /**
     * Transaction Status
     */
    private final String status;

    private final boolean retryGlobal;

    private boolean retryBranch;

    /**
     * Instantiates a new Global transaction event.
     *
     * @param id            the id
     * @param role          the role
     * @param name          the name
     * @param applicationId the application id
     * @param group         the group
     * @param beginTime     the begin time
     * @param endTime       the end time
     * @param status        the status
     * @param retryGlobal   the retry(1. delay delete global session 2. asyn retry branch session)
     * @param retryBranch   retry branch session
     */
    public GlobalTransactionEvent(long id, String role, String name, String applicationId, String group, Long beginTime, Long endTime, String status, boolean retryGlobal, boolean retryBranch) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.applicationId = applicationId;
        this.group = group;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.status = status;
        this.retryGlobal = retryGlobal;
        this.retryBranch = retryBranch;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Gets role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets group.
     *
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Gets begin time.
     *
     * @return the begin time
     */
    public Long getBeginTime() {
        return beginTime;
    }

    /**
     * Gets end time.
     *
     * @return the end time
     */
    public Long getEndTime() {
        return endTime;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Is retry boolean.
     *
     * @return the boolean
     */
    public boolean isRetryGlobal() {
        return retryGlobal;
    }

    /**
     * Is retry branch boolean.
     *
     * @return the boolean
     */
    public boolean isRetryBranch() {
        return retryBranch;
    }

    @Override
    public String toString() {
        return "GlobalTransactionEvent{" + "id=" + id + ", role='" + role + '\'' + ", name='" + name + '\''
            + ", applicationId='" + applicationId + '\'' + ", group='" + group + '\'' + ", beginTime=" + beginTime
            + ", endTime=" + endTime + ", status='" + status + '\'' + ", retryGlobal=" + retryGlobal + ", retryBranch="
            + retryBranch + '}';
    }
}
