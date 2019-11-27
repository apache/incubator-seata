package io.seata.tm.api.transaction;

/**
 * @author ruqinhu
 * @date 2019/11/27
 */
public class MyIllegalArgumentException extends IllegalArgumentException {

    public MyIllegalArgumentException(String msg) {
        super(msg);
    }
}
