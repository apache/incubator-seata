package io.seata.core.rpc.hook;


/**
 * @author longchenming
 * @date 2021/12/20 10:49
 * @desc
 */
public interface NettyClientTimeoutCheckerHook {

    void doBeforeChecker(String transactionServiceGroup);

    void doAfterChecker(String transactionServiceGroup);

}
