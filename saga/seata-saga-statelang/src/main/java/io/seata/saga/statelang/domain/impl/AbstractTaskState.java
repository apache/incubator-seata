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
package io.seata.saga.statelang.domain.impl;

import java.util.List;
import java.util.Map;

import io.seata.common.util.StringUtils;
import io.seata.saga.statelang.domain.TaskState;

/**
 * The state of the execution task (abstract class), the specific task to be executed is determined by the subclass
 *
 * @author lorne.cl
 */
public abstract class AbstractTaskState extends BaseState implements TaskState {

    private String compensateState;
    private boolean isForCompensation;
    private boolean isForUpdate;
    private List<Retry> retry;
    private List<ExceptionMatch> catches;
    private List<Object> input;
    private Map<String, Object> output;
    private Map<String, String> status;//Map<String/* expression */, String /* status */>
    private boolean isPersist = true;

    @Override
    public String getCompensateState() {
        return compensateState;
    }

    public void setCompensateState(String compensateState) {
        this.compensateState = compensateState;

        if (StringUtils.isNotBlank(this.compensateState)) {
            setForUpdate(true);
        }
    }

    @Override
    public boolean isForCompensation() {
        return isForCompensation;
    }

    public void setForCompensation(boolean isForCompensation) {
        this.isForCompensation = isForCompensation;
    }

    @Override
    public boolean isForUpdate() {
        return this.isForUpdate;
    }

    public void setForUpdate(boolean isForUpdate) {
        this.isForUpdate = isForUpdate;
    }

    @Override
    public List<Retry> getRetry() {
        return retry;
    }

    public void setRetry(List<Retry> retry) {
        this.retry = retry;
    }

    @Override
    public List<ExceptionMatch> getCatches() {
        return catches;
    }

    public void setCatches(List<ExceptionMatch> catches) {
        this.catches = catches;
    }

    public List<Object> getInput() {
        return input;
    }

    public void setInput(List<Object> input) {
        this.input = input;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public boolean isPersist() {
        return isPersist;
    }

    public void setPersist(boolean persist) {
        isPersist = persist;
    }

    @Override
    public Map<String, String> getStatus() {
        return status;
    }

    public void setStatus(Map<String, String> status) {
        this.status = status;
    }

    public static class RetryImpl implements Retry {

        private List<String> exceptions;
        private List<Class<? extends Exception>> exceptionClasses;
        private double intervalSeconds;
        private int maxAttempts;
        private double backoffRate;

        @Override
        public List<String> getExceptions() {
            return exceptions;
        }

        public void setExceptions(List<String> exceptions) {
            this.exceptions = exceptions;
        }

        @Override
        public List<Class<? extends Exception>> getExceptionClasses() {
            return exceptionClasses;
        }

        @Override
        public void setExceptionClasses(List<Class<? extends Exception>> exceptionClasses) {
            this.exceptionClasses = exceptionClasses;
        }

        @Override
        public double getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(double intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        @Override
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        @Override
        public double getBackoffRate() {
            return backoffRate;
        }

        public void setBackoffRate(double backoffRate) {
            this.backoffRate = backoffRate;
        }
    }

    public static class ExceptionMatchImpl implements ExceptionMatch {

        List<String> exceptions;
        List<Class<? extends Exception>> exceptionClasses;
        String next;

        @Override
        public List<String> getExceptions() {
            return exceptions;
        }

        public void setExceptions(List<String> exceptions) {
            this.exceptions = exceptions;
        }

        @Override
        public List<Class<? extends Exception>> getExceptionClasses() {
            return exceptionClasses;
        }

        @Override
        public void setExceptionClasses(List<Class<? extends Exception>> exceptionClasses) {
            this.exceptionClasses = exceptionClasses;
        }

        @Override
        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }
    }
}