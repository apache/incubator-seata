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
package io.seata.saga.engine.evaluation.exception;

import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.evaluation.Evaluator;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.statelang.domain.DomainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Exception match evaluator
 *
 * @author lorne.cl
 */
public class ExceptionMatchEvaluator implements Evaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMatchEvaluator.class);

    private String exceptionString;

    private Class<Exception> exceptionClass;

    private String rootObjectName = DomainConstants.VAR_NAME_CURRENT_EXCEPTION;

    @Override
    public boolean evaluate(Map<String, Object> variables) {

        Object eObj = variables.get(getRootObjectName());
        if (eObj != null && (eObj instanceof Exception) && StringUtils.hasText(exceptionString)) {

            Exception e = (Exception)eObj;

            String exceptionClassName = e.getClass().getName();
            if (exceptionClassName.equals(exceptionString)) {
                return true;
            }
            try {
                if (exceptionClass.isAssignableFrom(e.getClass())) {
                    return true;
                }
            } catch (Exception e1) {
                LOGGER.error("Exception Match failed. expression[{}]", exceptionString, e1);
            }
        }

        return false;
    }

    public String getExceptionString() {
        return exceptionString;
    }

    @SuppressWarnings("unchecked")
    public void setExceptionString(String exceptionString) {
        this.exceptionString = exceptionString;
        try {
            this.exceptionClass = (Class<Exception>)Class.forName(exceptionString);
        } catch (ClassNotFoundException e) {
            throw new EngineExecutionException(e, exceptionString + " is not a Exception Class",
                FrameworkErrorCode.NotExceptionClass);
        }
    }

    public Class<Exception> getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(Class<Exception> exceptionClass) {
        this.exceptionClass = exceptionClass;
        this.exceptionString = exceptionClass.getName();
    }

    public String getRootObjectName() {
        return rootObjectName;
    }

    public void setRootObjectName(String rootObjectName) {
        this.rootObjectName = rootObjectName;
    }
}