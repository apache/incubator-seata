package io.seata.core.rpc.hook;


/**
 * @author longchenming
 */
public interface NettyClientTimeoutCheckerHook {

    void doBeforeChecker(String transactionServiceGroup);

    void doAfterChecker(String transactionServiceGroup);

}
