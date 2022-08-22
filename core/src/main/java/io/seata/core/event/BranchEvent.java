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

import io.seata.core.model.BranchType;

/**
 * Event data for branch.
 */
public class BranchEvent implements Event {
    public static final String ROLE_TM = "tm";

    public static final String ROLE_RM = "rm";

    private long id;

    private final String role;

    private final String name;

    private String applicationId;

    private String group;

    private final Long beginTime;

    private final Long endTime;

    private String resourceGroupId;

    private String resourceId;

    private String lockKey;

    private BranchType branchType;


    private final String status;

    private final boolean retryGlobal;

    private boolean retryBranch;

    public BranchEvent(String role, String name, String applicationId, String group, Long beginTime, Long endTime, BranchType branchType, String status, boolean retryGlobal, boolean retryBranch) {
        this.role = role;
        this.name = name;
        this.applicationId = applicationId;
        this.group = group;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.branchType = branchType;
        this.status = status;
        this.retryGlobal = retryGlobal;
        this.retryBranch = retryBranch;
    }

    public BranchEvent(long id, String role, String name, String applicationId, String group,
                       Long beginTime, Long endTime, String resourceGroupId, String resourceId,
                       String lockKey, BranchType branchType, String status, boolean retryGlobal, boolean retryBranch) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.applicationId = applicationId;
        this.group = group;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.resourceGroupId = resourceGroupId;
        this.resourceId = resourceId;
        this.lockKey = lockKey;
        this.branchType = branchType;
        this.status = status;
        this.retryGlobal = retryGlobal;
        this.retryBranch = retryBranch;
    }

    public long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getGroup() {
        return group;
    }

    public Long getBeginTime() {
        return beginTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRetryGlobal() {
        return retryGlobal;
    }

    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getLockKey() {
        return lockKey;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public boolean isRetryBranch() {
        return retryBranch;
    }

    @Override
    public String toString() {
        return "BranchEvent{" +
                "id=" + id +
                ", role='" + role + '\'' +
                ", name='" + name + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", group='" + group + '\'' +
                ", beginTime=" + beginTime +
                ", endTime=" + endTime +
                ", resourceGroupId='" + resourceGroupId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", lockKey='" + lockKey + '\'' +
                ", branchType=" + branchType +
                ", status='" + status + '\'' +
                ", retryGlobal=" + retryGlobal +
                ", retryBranch=" + retryBranch +
                '}';
    }
}
