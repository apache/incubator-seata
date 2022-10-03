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
package io.seata.console.handler;

import io.seata.console.exception.ConsoleException;
import io.seata.console.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:12
 * @description ExceptionHandler in console module
 */

@RestControllerAdvice
public class ConsoleExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result exceptionHandle(Exception e) {
        return Result.error();
    }

    @ExceptionHandler(ConsoleException.class)
    public Result consoleExceptionHandle(ConsoleException e) {
        return Result.result(e.getCode());
    }
}
