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
package io.seata.server.session;

/**
 * The type Session condition.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /12/13
 */
public class SessionCondition {

    private long overTimeAliveMills;

    /**
     * Instantiates a new Session condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public SessionCondition( long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
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
