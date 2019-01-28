/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.server.session;

import com.alibaba.fescar.core.model.GlobalStatus;

/**
 * The type Session condition.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /12/13 14:47
 * @FileName: SessionCondition
 * @Description:
 */
public class SessionCondition {
    private GlobalStatus status;
    private long overTimeAliveMills;

    /**
     * Instantiates a new Session condition.
     *
     * @param status             the status
     * @param overTimeAliveMills the over time alive mills
     */
    public SessionCondition(GlobalStatus status, long overTimeAliveMills) {
        this.status = status;
        this.overTimeAliveMills = overTimeAliveMills;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public GlobalStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(GlobalStatus status) {
        this.status = status;
    }

    /**
     * Gets over time alive mills.
     *
     * @return the over time alive mills
     */
    public long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    /**
     * Sets over time alive mills.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public void setOverTimeAliveMills(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }
}
