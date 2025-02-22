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
package org.apache.seata.server.console.aop;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.exception.ConsoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Component
public class GlobalExceptionHandlerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlerAdvice.class);

    @ExceptionHandler(ConsoleException.class)
    @ResponseBody
    public SingleResult<Void> handlerConsoleException(ConsoleException ex) {
        LOGGER.error("console error, reason: {}", ex.getLogMessage(), ex);
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public SingleResult<Void> handlerIllegalArgumentException(IllegalArgumentException ex) {
        LOGGER.error("IllegalArgumentException: ", ex);
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public SingleResult<Void> handlerIllegalStateException(IllegalStateException ex) {
        LOGGER.error("IllegalStateException: ", ex);
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(ShouldNeverHappenException.class)
    @ResponseBody
    public SingleResult<Void> handlerShouldNeverHappenException(ShouldNeverHappenException ex) {
        LOGGER.error("ShouldNeverHappenException: ", ex);
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(FrameworkException.class)
    @ResponseBody
    public SingleResult<Void> handlerFrameworkException(FrameworkException ex) {
        LOGGER.error("FrameworkException: ", ex);
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public SingleResult<Void> handleException(Exception ex) {
        LOGGER.error("Exception: ", ex);
        return SingleResult.failure(ex.getMessage());
    }
}
