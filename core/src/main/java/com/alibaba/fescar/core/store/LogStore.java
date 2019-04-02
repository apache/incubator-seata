package com.alibaba.fescar.core.store;

/**
 * the transaction log store
 *
 * @author zhangsen
 * @data 2019 /3/26
 */
public interface LogStore {

    /**
     * 查询全局事务记录
     *
     * @param transactionId the transaction id
     * @return global transaction do
     */
    GlobalTransactionDO getGlobalTransactionDO(String transactionId);

    /**
     * Write global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return boolean
     */
    boolean writeGlobalTransactionDO(GlobalTransactionDO globalTransactionDO);

    /**
     * Write branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return boolean
     */
    boolean writeBranchTransactionDO(BranchTransactionDO branchTransactionDO);


}
