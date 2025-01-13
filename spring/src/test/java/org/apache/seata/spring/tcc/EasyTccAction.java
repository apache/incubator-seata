package org.apache.seata.spring.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

/**
 *
 * @date 2025年01月12日23:17
 */

public interface EasyTccAction {

    /**
     * Prepare boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean prepare(BusinessActionContext actionContext);

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * Rollback boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean rollback(BusinessActionContext actionContext);

}
