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

/**
 * Execution Status
 *
 * @author lorne.cl
 */
public enum ExecutionStatus {

    /**
     * Running
     */
    RU("Running"),

    /**
     * Succeed
     */
    SU("Succeed"),

    /**
     * Failed
     */
    FA("Failed"),

    /**
     * Unknown
     */
    UN("Unknown"),

    /**
     * Skipped
     */
    SK("Skipped");

    private String statusString;

    private ExecutionStatus(String statusString) {
        this.statusString = statusString;
    }

    public String getStatusString() {
        return statusString;
    }
}