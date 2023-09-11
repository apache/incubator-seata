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

package io.seata.saga.statelang.builder.prop;

import java.util.Collection;

/***
 * Task property builder.
 *
 * @param <P> property builder type
 * @author ptyin
 */
public interface TaskPropertyBuilder<P extends TaskPropertyBuilder<P>> {
    /**
     * Configure compensation state.
     *
     * @param compensationState name of compensation state
     * @return builder for chaining
     */
    P withCompensationState(String compensationState);

    /**
     * Configure if this state is used to compensate another state.
     *
     * @param forCompensation if is for compensation or not
     * @return builder for chaining
     */
    P withForCompensation(boolean forCompensation);

    /**
     * Configure if this state will update data
     *
     * @param forUpdate if is for update
     * @return builder for chaining
     */
    P withForUpdate(boolean forUpdate);

    /**
     * Configure retry strategy. If the state has multiple retry strategies, use following way to build:
     * <pre>
     * {@code
     * stateBuilder
     *     .withRetry()
     *         .withExceptions(Arrays.asList(IllegalArgumentException.class))
     *         .withIntervalSeconds(1)
     *         .and()
     *     .withRetry()
     *         .withExceptions(Arrays.asList(NullPointerException.class))
     *         .withIntervalSeconds(10)
     * // ...
     * }
     * </pre>
     *
     * @return retry builder
     */
    TaskPropertyBuilder.RetryBuilder<P> withOneRetry();

    /**
     * Configure exception catching rules. If the state has multiple catching rules, use following way to build:
     * <pre>
     * {@code
     * stateBuilder
     *     .withCatches()
     *         .withExceptions(Arrays.asList(IllegalArgumentException.class))
     *         .withIntervalSeconds(1)
     *         .and()
     *     .withCatches()
     *         .withExceptions(Arrays.asList(NullPointerException.class))
     *         .withIntervalSeconds(10)
     * // ...
     * }
     * </pre>
     *
     * @return exception match builder
     */
    TaskPropertyBuilder.ExceptionMatchBuilder<P> withOneCatch();

    /**
     * Configure execution status.
     *
     * @param expression expression to evaluated
     * @param status matched status
     * @return builder for chaining
     */
    P withOneStatus(String expression, String status);

    /**
     * Configure loop strategy.
     *
     * @return builder for chaining
     */
    TaskPropertyBuilder.LoopBuilder<P> withLoop();

    interface ChildBuilder<B> {
        /**
         * Return parent builder
         *
         * @return parent builder
         */
        B and();
    }

    interface RetryBuilder<B> extends TaskPropertyBuilder.ChildBuilder<B> {
        /**
         * Configure exception classes to capture
         *
         * @param exceptions exception classes
         * @return retry builder for chaining
         */
        TaskPropertyBuilder.RetryBuilder<B> withExceptions(Collection<Class<? extends Exception>> exceptions);

        /**
         * Configure interval
         *
         * @param intervalSeconds interval in seconds
         * @return retry builder for chaining
         */
        TaskPropertyBuilder.RetryBuilder<B> withIntervalSeconds(double intervalSeconds);

        /**
         * Configure max attempts
         *
         * @param maxAttempts max count of attempts
         * @return retry builder for chaining
         */
        TaskPropertyBuilder.RetryBuilder<B> withMaxAttempts(int maxAttempts);

        /**
         * Configure backoff rate
         *
         * @param backoffRate backoff rate
         * @return retry builder for chaining
         */
        TaskPropertyBuilder.RetryBuilder<B> withBackoffRate(double backoffRate);
    }

    interface ExceptionMatchBuilder<B> extends TaskPropertyBuilder.ChildBuilder<B> {
        /**
         * Configure exception classes to capture.
         *
         * @param exceptions exception classes
         * @return exception match builder for chaining
         */
        TaskPropertyBuilder.ExceptionMatchBuilder<B> withExceptions(Collection<Class<? extends Exception>> exceptions);

        /**
         * Configure next state when exception matched.
         *
         * @param next name of next state
         * @return exception match builder for chaining
         */
        TaskPropertyBuilder.ExceptionMatchBuilder<B> withNext(String next);
    }

    interface LoopBuilder<B> extends TaskPropertyBuilder.ChildBuilder<B> {
        /**
         * Configure max parallelism.
         *
         * @param parallel max parallelism, i.e. max count of threads at a specific moment
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withParallel(int parallel);

        /**
         * Configure collection object name.
         *
         * @param collection collection name
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withCollection(String collection);

        /**
         * Configure element variable name.
         *
         * @param elementVariableName element variable name
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withElementVariableName(String elementVariableName);

        /**
         * Configure element variable index name, default loopCounter.
         *
         * @param elementIndexName element index name
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withElementIndexName(String elementIndexName);

        /**
         * Configure completion condition, default nrOfInstances == nrOfCompletedInstances.
         *
         * @param completionCondition completion condition
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withCompletionCondition(String completionCondition);

        /**
         * Configure the name of loop result, default loopResult.
         *
         * @param resultName result name
         * @return loop builder for chaining
         */
        TaskPropertyBuilder.LoopBuilder<B> withResultName(String resultName);

        /**
         * Configure the name of number of instances, default nrOfInstances.
         *
         * @param numberOfInstancesName the name of number of instances
         * @return loop builder for chaining
         */
        LoopBuilder<B> withNumberOfInstancesName(String numberOfInstancesName);

        /**
         * Configure the name of number of active instances, default nrOfActiveInstances.
         *
         * @param numberOfActiveInstancesName the name of number of active instances
         * @return loop builder for chaining
         */
        LoopBuilder<B> withNumberOfActiveInstancesName(String numberOfActiveInstancesName);

        /**
         * Configure the name of number of active instances, default nrOfCompletedInstances.
         *
         * @param numberOfCompletedInstancesName the number of completed instances name
         * @return loop builder for chaining
         */
        LoopBuilder<B> withNumberOfCompletedInstancesName(String numberOfCompletedInstancesName);
    }
}
