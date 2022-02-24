package io.seata.server.storage.redis.store;

import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalTransactionDO;

/**
 * redis Global Transaction data object
 *
 * @author Bughue
 */
public class RedisGlobalTransactionVO {

    private GlobalTransactionDO globalTransactionDO;

    private GlobalStatus expectedStatus;

    /**
     * Gets globalTransactionDO.
     *
     * @return the globalTransactionDO.
     */
    public GlobalTransactionDO getGlobalTransactionDO() {
        return globalTransactionDO;
    }

    /**
     * Sets globalTransactionDO.
     *
     * @param globalTransactionDO the globalTransactionDO
     */
    public void setGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        this.globalTransactionDO = globalTransactionDO;
    }

    /**
     * Gets expected status.
     *
     * @return the expected status.
     */
    public GlobalStatus getExpectedStatus() {
        return expectedStatus;
    }

    /**
     * Sets expected status.
     *
     * @param expectedStatus the expected status
     */
    public void setExpectedStatus(GlobalStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }
}
