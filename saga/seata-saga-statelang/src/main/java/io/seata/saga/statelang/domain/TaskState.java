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

import java.util.List;
import java.util.Map;

/**
 * A state used to execute a task
 *
 * @author lorne.cl
 */
public interface TaskState extends State {

    /**
     * get compensate state
     *
     * @return the compensate state
     */
    String getCompensateState();

    /**
     * Is this state is used to compensate an other state, default false
     *
     * @return is for compensate
     */
    boolean isForCompensation();

    /**
     * Is this state will update data? default false
     *
     * @return is for update
     */
    boolean isForUpdate();

    /**
     * retry strategy
     *
     * @return retry list
     */
    List<Retry> getRetry();

    /**
     * exception handling strategy
     *
     * @return exception list
     */
    List<ExceptionMatch> getCatches();

    /**
     * Execution state determination rule
     *
     * @return execution state
     */
    Map<String, String> getStatus();

    /**
     * loop strategy
     *
     * @return the loop strategy
     */
    Loop getLoop();

    /**
     * retry strategy
     */
    interface Retry {

        /**
         * exceptions
         *
         * @return the exception list
         */
        List<String> getExceptions();

        /**
         * exception classes
         *
         * @return exception list
         */
        List<Class<? extends Exception>> getExceptionClasses();

        /**
         * set exception classes
         * @param exceptionClasses exception class
         */
        void setExceptionClasses(List<Class<? extends Exception>> exceptionClasses);

        /**
         * getIntervalSeconds
         *
         * @return the interval seconds
         */
        double getIntervalSeconds();

        /**
         * getMaxAttempts
         *
         * @return the max attempts
         */
        int getMaxAttempts();

        /**
         * get BackoffRate, default 1
         *
         * @return the backoff rate
         */
        double getBackoffRate();
    }

    /**
     * exception match
     */
    interface ExceptionMatch {

        /**
         * exceptions
         *
         * @return exception list
         */
        List<String> getExceptions();

        /**
         * exception classes
         *
         * @return exception classes
         */
        List<Class<? extends Exception>> getExceptionClasses();

        /**
         * set exception classes
         * @param exceptionClasses exception class
         */
        void setExceptionClasses(List<Class<? extends Exception>> exceptionClasses);

        /**
         * next state name
         *
         * @return the next state name
         */
        String getNext();
    }

    /**
     * status match
     */
    interface StatusMatch {

        /**
         * status
         *
         * @return the task execution status
         */
        ExecutionStatus getStatus();

        /**
         * expression
         *
         * @return the task execution expression
         */
        String getExpression();

        /**
         * expression type, default(SpringEL)|exception
         *
         * @return the task expression type
         */
        String getExpressionType();
    }

    /**
     * loop strategy
     */
    interface Loop {

        /**
         * parallel size, default 1
         *
         * @return the task parallel size
         */
        int getParallel();

        /**
         * collection object name
         *
         * @return the collection object name
         */
        String getCollection();

        /**
         * element variable name
         *
         * @return the element variable name
         */
        String getElementVariableName();

        /**
         * element variable index name, default loopCounter
         *
         * @return the element variable index name
         */
        String getElementIndexName();

        /**
         * completion condition, default nrOfInstances == nrOfCompletedInstances
         *
         * @return the completion condition
         */
        String getCompletionCondition();
    }
}