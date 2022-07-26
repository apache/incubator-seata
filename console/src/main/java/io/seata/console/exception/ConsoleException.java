package io.seata.console.exception;

import io.seata.console.constant.Code;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:09
 * @description
 */

public class ConsoleException extends RuntimeException{

    private Code code;

    public ConsoleException(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
