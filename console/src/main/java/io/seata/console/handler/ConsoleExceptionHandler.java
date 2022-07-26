package io.seata.console.handler;

import io.seata.console.exception.ConsoleException;
import io.seata.console.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:12
 * @description
 */

@RestControllerAdvice
public class ConsoleExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result exceptionHandle(Exception e) {
        e.printStackTrace();
        return Result.error();
    }

    @ExceptionHandler(ConsoleException.class)
    public Result consoleExceptionHandle(ConsoleException e) {
        e.printStackTrace();
        return Result.result(e.getCode());
    }
}
