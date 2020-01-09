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
package io.seata.saga.statelang.parser.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.seata.saga.statelang.domain.TaskState.ExceptionMatch;
import io.seata.saga.statelang.domain.TaskState.Retry;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import io.seata.saga.statelang.domain.impl.AbstractTaskState.ExceptionMatchImpl;
import io.seata.saga.statelang.domain.impl.AbstractTaskState.RetryImpl;

/**
 * AbstractTaskStateParser
 *
 * @author lorne.cl
 */
public abstract class AbstractTaskStateParser extends BaseStatePaser {

    protected void parseTaskAttributes(AbstractTaskState state, Object node) {

        parseBaseAttributes(state, node);

        Map<String, Object> nodeMap = (Map<String, Object>) node;

        state.setCompensateState((String) nodeMap.get("CompensateState"));
        state.setForCompensation(Boolean.TRUE.equals(nodeMap.get("IsForCompensation")));
        state.setForUpdate(Boolean.TRUE.equals(nodeMap.get("IsForUpdate")));
        Object isPersist = nodeMap.get("IsPersist");
        if (Boolean.FALSE.equals(isPersist)) {
            state.setPersist(false);
        }

        List<Object> retryList = (List<Object>) nodeMap.get("Retry");
        if (retryList != null) {
            state.setRetry(parseRetry(retryList));
        }

        List<Object> catchList = (List<Object>) nodeMap.get("Catch");
        if (catchList != null) {
            state.setCatches(parseCatch(catchList));
        }

        List<Object> inputList = (List<Object>) nodeMap.get("Input");
        if (inputList != null) {
            state.setInput(inputList);
        }

        Map<String, Object> outputMap = (Map<String, Object>) nodeMap.get("Output");
        if (outputMap != null) {
            state.setOutput(outputMap);
        }

        Map<String/* expression */, String /* status */> statusMap = (Map<String, String>) nodeMap.get("Status");
        if (statusMap != null) {
            state.setStatus(statusMap);
        }
    }

    protected List<Retry> parseRetry(List<Object> retryList) {
        if (retryList != null) {
            List<Retry> retries = new ArrayList<>(retryList.size());
            for (Object retryObj : retryList) {
                Map<String, Object> retryMap = (Map<String, Object>) retryObj;
                RetryImpl retry = new RetryImpl();
                retry.setExceptions((List<String>) retryMap.get("Exceptions"));

                Object intervalSeconds = retryMap.get("IntervalSeconds");
                if (intervalSeconds != null && intervalSeconds instanceof Number) {
                    retry.setIntervalSeconds(((Number) intervalSeconds).doubleValue());
                }

                retry.setMaxAttempts((Integer) retryMap.get("MaxAttempts"));

                Object backoffRate = retryMap.get("BackoffRate");
                if (backoffRate != null && backoffRate instanceof Number) {
                    retry.setBackoffRate(((Number) backoffRate).doubleValue());
                }

                retries.add(retry);
            }
            return retries;
        }
        return new ArrayList<>(0);
    }

    protected List<ExceptionMatch> parseCatch(List<Object> catchList) {

        List<ExceptionMatch> exceptionMatchList = new ArrayList<>(catchList.size());
        for (Object exceptionMatchObj : catchList) {
            Map<String, Object> exceptionMatchMap = (Map<String, Object>) exceptionMatchObj;
            ExceptionMatchImpl exceptionMatch = new ExceptionMatchImpl();
            exceptionMatch.setExceptions((List<String>) exceptionMatchMap.get("Exceptions"));
            exceptionMatch.setNext((String) exceptionMatchMap.get("Next"));

            exceptionMatchList.add(exceptionMatch);
        }
        return exceptionMatchList;
    }
}