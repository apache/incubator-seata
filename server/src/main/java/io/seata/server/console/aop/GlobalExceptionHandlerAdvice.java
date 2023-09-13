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
package io.seata.server.console.aop;

import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.console.result.SingleResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Component
public class GlobalExceptionHandlerAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public SingleResult<Void> handlerIllegalArgumentException(IllegalArgumentException ex) {
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public SingleResult<Void> handlerIllegalStateException(IllegalStateException ex) {
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(ShouldNeverHappenException.class)
    @ResponseBody
    public SingleResult<Void> handlerShouldNeverHappenException(ShouldNeverHappenException ex) {
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(FrameworkException.class)
    @ResponseBody
    public SingleResult<Void> handlerFrameworkException(FrameworkException ex) {
        return SingleResult.failure(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public SingleResult<Void> handleException(Exception ex) {
        return SingleResult.failure("There was an error with the system, please try again later!");
    }
}
