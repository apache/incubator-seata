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

import io.seata.core.model.GlobalStatus;

/**
 * Event data for global transaction.
 *
 * @author zhengyangyong
 */
public class GlobalTransactionEvent implements Event {
    public static final String ROLE_TC = "tc";

    public static final String ROLE_TM = "tm";

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
    private final GlobalStatus status;

    public long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public Long getBeginTime() {
        return beginTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public GlobalStatus getStatus() {
        return status;
    }

    public GlobalTransactionEvent(long id, String role, String name, Long beginTime, Long endTime,
                                  GlobalStatus status) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.status = status;
    }
}
