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

import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.builder.prop.TaskPropertyBuilder;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract task state builder to inherit.
 *
 * @param <P> builder type
 * @param <S> state type
 * @author ptyin
 */
public abstract class AbstractTaskStateBuilder
        <P extends BasicPropertyBuilder<P> & TaskPropertyBuilder<P>, S extends State>
        extends BaseStateBuilder<P, S>
        implements BasicPropertyBuilder<P>, TaskPropertyBuilder<P> {

    public AbstractTaskStateBuilder() {
        AbstractTaskState state = (AbstractTaskState) getState();
        // Do some default setup
        state.setForCompensation(false);
        state.setForUpdate(false);
        state.setPersist(true);
    }

    @Override
    public P withCompensationState(String compensationState) {
        ((AbstractTaskState) getState()).setCompensateState(compensationState);
        return getPropertyBuilder();
    }

    @Override
    public P withForCompensation(boolean forCompensation) {
        ((AbstractTaskState) getState()).setForCompensation(forCompensation);
        return getPropertyBuilder();
    }

    @Override
    public P withForUpdate(boolean forUpdate) {
        ((AbstractTaskState) getState()).setForUpdate(forUpdate);
        return getPropertyBuilder();
    }

    @Override
    public P withPersist(boolean persist) {
        ((AbstractTaskState) getState()).setPersist(persist);
        return getPropertyBuilder();
    }

    @Override
    public P withRetryPersistModeUpdate(boolean retryPersistModeUpdate) {
        ((AbstractTaskState) getState()).setRetryPersistModeUpdate(retryPersistModeUpdate);
        return getPropertyBuilder();
    }

    @Override
    public P withCompensatePersistModeUpdate(boolean compensatePersistModeUpdate) {
        ((AbstractTaskState) getState()).setCompensatePersistModeUpdate(compensatePersistModeUpdate);
        return getPropertyBuilder();
    }

    @Override
    public RetryBuilder<P> withOneRetry() {
        return new RetryBuilderImpl();
    }

    @Override
    public ExceptionMatchBuilder<P> withOneCatch() {
        return new ExceptionMatchBuilderImpl();
    }

    @Override
    public P withInput(Collection<Object> input) {
        ((AbstractTaskState) getState()).setInput(new ArrayList<>(input));
        return getPropertyBuilder();
    }

    @Override
    public P withOutput(Map<String, Object> output) {
        AbstractTaskState state = ((AbstractTaskState) getState());
        if (state.getOutput() == null) {
            state.setOutput(new LinkedHashMap<>());
        }
        state.getOutput().putAll(output);
        return getPropertyBuilder();
    }

    @Override
    public P withOneOutput(String variable, Object expression) {
        AbstractTaskState state = ((AbstractTaskState) getState());
        if (state.getOutput() == null) {
            state.setOutput(new LinkedHashMap<>());
        }
        state.getOutput().put(variable, expression);
        return getPropertyBuilder();
    }

    @Override
    public P withStatus(Map<String, String> status) {
        AbstractTaskState state = ((AbstractTaskState) getState());
        if (state.getStatus() == null) {
            state.setStatus(new LinkedHashMap<>());
        }
        state.getStatus().putAll(status);
        return getPropertyBuilder();
    }

    @Override
    public P withOneStatus(String expression, String status) {
        AbstractTaskState state = ((AbstractTaskState) getState());
        if (state.getStatus() == null) {
            state.setStatus(new LinkedHashMap<>());
        }
        state.getStatus().put(expression, status);
        return getPropertyBuilder();
    }

    @Override
    public LoopBuilder<P> withLoop() {
        return new LoopBuilderImpl();
    }

    public class RetryBuilderImpl implements RetryBuilder<P> {

        private final AbstractTaskState.RetryImpl oneRetry = new AbstractTaskState.RetryImpl();

        @Override
        public P and() {
            AbstractTaskState state = ((AbstractTaskState) getState());
            if (state.getRetry() == null) {
                state.setRetry(new ArrayList<>());
            }
            state.getRetry().add(oneRetry);
            return getPropertyBuilder();
        }

        @Override
        public RetryBuilder<P> withExceptions(Collection<Class<? extends Exception>> exceptions) {
            oneRetry.setExceptions(exceptions.stream().map(Class::getName).collect(Collectors.toList()));
            return this;
        }

        @Override
        public RetryBuilder<P> withIntervalSeconds(double intervalSeconds) {
            oneRetry.setIntervalSeconds(intervalSeconds);
            return this;
        }

        @Override
        public RetryBuilder<P> withMaxAttempts(int maxAttempts) {
            oneRetry.setMaxAttempts(maxAttempts);
            return this;
        }

        @Override
        public RetryBuilder<P> withBackoffRate(double backoffRate) {
            oneRetry.setBackoffRate(backoffRate);
            return this;
        }
    }

    public class ExceptionMatchBuilderImpl implements ExceptionMatchBuilder<P> {

        private final AbstractTaskState.ExceptionMatchImpl oneCatch = new AbstractTaskState.ExceptionMatchImpl();

        @Override
        public P and() {
            AbstractTaskState state = ((AbstractTaskState) getState());
            if (state.getCatches() == null) {
                state.setCatches(new ArrayList<>());
            }
            state.getCatches().add(oneCatch);
            return getPropertyBuilder();
        }

        @Override
        public ExceptionMatchBuilder<P> withExceptions(Collection<Class<? extends Exception>> exceptions) {
            oneCatch.setExceptions(exceptions.stream().map(Class::getName).collect(Collectors.toList()));
            return this;
        }

        @Override
        public ExceptionMatchBuilder<P> withNext(String next) {
            oneCatch.setNext(next);
            return this;
        }
    }

    public class LoopBuilderImpl implements LoopBuilder<P> {

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
        public P and() {
            AbstractTaskState state = ((AbstractTaskState) getState());
            state.setLoop(loop);
            return getPropertyBuilder();
        }

        @Override
        public LoopBuilder<P> withParallel(int parallel) {
            loop.setParallel(parallel);
            return this;
        }

        @Override
        public LoopBuilder<P> withCollection(String collection) {
            loop.setCollection(collection);
            return this;
        }

        @Override
        public LoopBuilder<P> withElementVariableName(String elementVariableName) {
            loop.setElementVariableName(elementVariableName);
            return this;
        }

        @Override
        public LoopBuilder<P> withElementIndexName(String elementIndexName) {
            loop.setElementIndexName(elementIndexName);
            return this;
        }

        @Override
        public LoopBuilder<P> withCompletionCondition(String completionCondition) {
            loop.setCompletionCondition(completionCondition);
            return this;
        }

        @Override
        public LoopBuilder<P> withResultName(String resultName) {
            loop.setResultName(resultName);
            return this;
        }

        @Override
        public LoopBuilder<P> withNumberOfInstancesName(String numberOfInstancesName) {
            loop.setNumberOfInstancesName(numberOfInstancesName);
            return this;
        }

        @Override
        public LoopBuilder<P> withNumberOfActiveInstancesName(String numberOfActiveInstancesName) {
            loop.setNumberOfActiveInstancesName(numberOfActiveInstancesName);
            return this;
        }

        @Override
        public LoopBuilder<P> withNumberOfCompletedInstancesName(String numberOfCompletedInstancesName) {
            loop.setNumberOfCompletedInstancesName(numberOfCompletedInstancesName);
            return this;
        }
    }
}
