package io.seata.rm;

import io.seata.core.model.GlobalLockConfig;

/**
 * executor to execute business logic that require global lock
 * @author selfishlover
 */
public interface GlobalLockExecutor {

    /**
     * execute business logic
     * @return business return
     * @throws Throwable whatever throw during execution
     */
    Object execute() throws Throwable;

    /**
     * global lock config info
     * @return
     */
    GlobalLockConfig getGlobalLockConfig();
}
