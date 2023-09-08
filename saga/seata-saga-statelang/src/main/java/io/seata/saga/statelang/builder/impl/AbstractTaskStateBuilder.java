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

package io.seata.saga.statelang.builder.impl;

import io.seata.saga.statelang.builder.StateBuilder;
import io.seata.saga.statelang.builder.TaskStateBuilder;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Abstract task state builder to inherit.
 *
 * @param <B> builder type
 * @param <S> state type
 * @author ptyin
 */
public abstract class AbstractTaskStateBuilder<B extends TaskStateBuilder<B> & StateBuilder<B, S>, S extends State>
        extends BaseStateBuilder<B, S> implements TaskStateBuilder<B> {

    protected AbstractTaskState state;

    public AbstractTaskStateBuilder() {
        state = (AbstractTaskState) getState();
        // Do some default setup
        state.setForCompensation(false);
        state.setForUpdate(false);
        state.setRetryPersistModeUpdate(false);
        state.setCompensatePersistModeUpdate(false);

        state.setPersist(true);
    }

    @Override
    public B withCompensationState(String compensationState) {
        state.setCompensateState(compensationState);
        return getBuilder();
    }

    @Override
    public B withForCompensation(boolean forCompensation) {
        state.setForUpdate(forCompensation);
        return getBuilder();
    }

    @Override
    public B withForUpdate(boolean forUpdate) {
        state.setForUpdate(forUpdate);
        return getBuilder();
    }

    @Override
    public RetryBuilder<B> withOneRetry() {
        return new RetryBuilderImpl();
    }

    @Override
    public ExceptionMatchBuilder<B> withOneCatch() {
        return new ExceptionMatchBuilderImpl();
    }

    @Override
    public B withOneStatus(String expression, String status) {
        if (state.getStatus() == null) {
            state.setStatus(new HashMap<>());
        }
        state.getStatus().put(expression, status);
        return getBuilder();
    }

    @Override
    public LoopBuilder<B> withLoop() {
        return new LoopBuilderImpl();
    }

    public class RetryBuilderImpl implements TaskStateBuilder.RetryBuilder<B> {

        private final AbstractTaskState.RetryImpl oneRetry = new AbstractTaskState.RetryImpl();

        @Override
        public B and() {
            if (state.getRetry() == null) {
                state.setRetry(new ArrayList<>());
            }
            state.getRetry().add(oneRetry);
            return getBuilder();
        }

        @Override
        public RetryBuilder<B> withExceptions(Collection<Class<? extends Exception>> exceptions) {
            oneRetry.setExceptions(exceptions.stream().map(Class::getName).collect(Collectors.toList()));
            oneRetry.setExceptionClasses(new ArrayList<>(exceptions));
            return this;
        }

        @Override
        public RetryBuilder<B> withIntervalSeconds(double intervalSeconds) {
            oneRetry.setIntervalSeconds(intervalSeconds);
            return this;
        }

        @Override
        public RetryBuilder<B> withMaxAttempts(int maxAttempts) {
            oneRetry.setMaxAttempts(maxAttempts);
            return this;
        }

        @Override
        public RetryBuilder<B> withBackoffRate(double backoffRate) {
            oneRetry.setBackoffRate(backoffRate);
            return this;
        }
    }

    public class ExceptionMatchBuilderImpl implements TaskStateBuilder.ExceptionMatchBuilder<B> {

        private final AbstractTaskState.ExceptionMatchImpl oneCatch = new AbstractTaskState.ExceptionMatchImpl();

        @Override
        public B and() {
            if (state.getCatches() == null) {
                state.setCatches(new ArrayList<>());
            }
            state.getCatches().add(oneCatch);
            return getBuilder();
        }

        @Override
        public ExceptionMatchBuilder<B> withExceptions(Collection<Class<? extends Exception>> exceptions) {
            oneCatch.setExceptions(exceptions.stream().map(Class::getName).collect(Collectors.toList()));
            oneCatch.setExceptionClasses(new ArrayList<>(exceptions));
            return this;
        }

        @Override
        public ExceptionMatchBuilder<B> withNext(String next) {
            oneCatch.setNext(next);
            return this;
        }
    }

    public class LoopBuilderImpl implements LoopBuilder<B> {

        private final AbstractTaskState.LoopImpl loop = new AbstractTaskState.LoopImpl();

        public LoopBuilderImpl() {
            // Do some default setup
            loop.setParallel(1);
            loop.setElementVariableName("loopElement");
            loop.setElementIndexName("loopCounter");
            loop.setResultName("loopResult");
            loop.setNumberOfInstancesName("nrOfInstances");
            loop.setNumberOfActiveInstancesName("nrOfActiveInstances");
            loop.setNumberOfCompletedInstancesName("nrOfCompletedInstances");
            loop.setCompletionCondition("[nrOfInstances] == [nrOfCompletedInstances]");
        }

        @Override
        public B and() {
            state.setLoop(loop);
            return getBuilder();
        }

        @Override
        public LoopBuilder<B> withParallel(int parallel) {
            loop.setParallel(parallel);
            return this;
        }

        @Override
        public LoopBuilder<B> withCollection(String collection) {
            loop.setCollection(collection);
            return this;
        }

        @Override
        public LoopBuilder<B> withElementVariableName(String elementVariableName) {
            loop.setElementVariableName(elementVariableName);
            return this;
        }

        @Override
        public LoopBuilder<B> withElementIndexName(String elementIndexName) {
            loop.setElementIndexName(elementIndexName);
            return this;
        }

        @Override
        public LoopBuilder<B> withCompletionCondition(String completionCondition) {
            loop.setCompletionCondition(completionCondition);
            return this;
        }

        @Override
        public LoopBuilder<B> withResultName(String resultName) {
            loop.setResultName(resultName);
            return this;
        }

        @Override
        public LoopBuilder<B> withNumberOfInstancesName(String numberOfInstancesName) {
            loop.setNumberOfInstancesName(numberOfInstancesName);
            return this;
        }

        @Override
        public LoopBuilder<B> withNumberOfActiveInstancesName(String numberOfActiveInstancesName) {
            loop.setNumberOfActiveInstancesName(numberOfActiveInstancesName);
            return this;
        }

        @Override
        public LoopBuilder<B> withNumberOfCompletedInstancesName(String numberOfCompletedInstancesName) {
            loop.setNumberOfCompletedInstancesName(numberOfCompletedInstancesName);
            return this;
        }
    }
}
