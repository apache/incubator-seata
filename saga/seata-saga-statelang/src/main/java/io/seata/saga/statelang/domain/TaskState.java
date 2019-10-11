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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A state used to execute a task
 * @author lorne.cl
 */
public interface TaskState extends State {

    /**
     * get compensate state
     * @return
     */
    String getCompensateState();

    /**
     * Is this state is used to compensate an other state，default false
     * @return
     */
    boolean isForCompensation();

    /**
     * Is this state will update data? default false
     * @return
     */
    boolean isForUpdate();

    /**
     * retry strategy
     * @return
     */
    Retry getRetry();

    /**
     * exception handling strategy
     * @return
     */
    List<ExceptionMatch> getCatches();

    /**
     * Execution state determination rule
     * @return
     */
    Map<String, String> getStatus();

    /**
     * retry strategy
     */
    interface Retry {

        /**
         * getIntervalSeconds
         * @return
         */
        int getIntervalSeconds();

        /**
         * getMaxAttempts
         * @return
         */
        int getMaxAttempts();

        /**
         * get BackoffRate，default 1
         * @return
         */
        BigDecimal getBackoffRate();
    }

    /**
     * exception match
     */
    interface ExceptionMatch {

        /**
         * exceptions
         * @return
         */
        List<String> getExceptions();

        /**
         * exception classes
         * @return
         */
        List<Class<? extends Exception>> getExceptionClasses();

        /**
         * next state name
         * @return
         */
        String getNext();
    }

    /**
     * status match
     */
    interface StatusMatch {

        /**
         * status
         * @return
         */
        ExecutionStatus getStatus();

        /**
         * expression
         * @return
         */
        String getExpression();

        /**
         * expression type, default(SpringEL)|exception
         * @return
         */
        String getExpressionType();
    }
}