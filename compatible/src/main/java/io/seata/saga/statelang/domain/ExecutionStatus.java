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

/**
 * Execution Status
 */
@Deprecated
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

    public static ExecutionStatus wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus target) {
        if (target == null) {
            return null;
        }
        switch (target) {
            case RU:
                return RU;
            case SU:
                return SU;
            case FA:
                return FA;
            case UN:
                return UN;
            case SK:
                return SK;
            default:
                throw new IllegalArgumentException("Cannot convert " + target.name());
        }
    }

    public org.apache.seata.saga.statelang.domain.ExecutionStatus unwrap() {
        switch (this) {
            case RU:
                return org.apache.seata.saga.statelang.domain.ExecutionStatus.RU;
            case SU:
                return org.apache.seata.saga.statelang.domain.ExecutionStatus.SU;
            case FA:
                return org.apache.seata.saga.statelang.domain.ExecutionStatus.FA;
            case UN:
                return org.apache.seata.saga.statelang.domain.ExecutionStatus.UN;
            case SK:
                return org.apache.seata.saga.statelang.domain.ExecutionStatus.SK;
            default:
                throw new IllegalArgumentException("Cannot convert " + this.name());
        }
    }
}
