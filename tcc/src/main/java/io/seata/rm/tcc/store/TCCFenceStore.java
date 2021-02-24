package io.seata.rm.tcc.store;

import javax.sql.DataSource;
import java.sql.Timestamp;

/**
 * The TCC Fence Store
 *
 * @author cebbank
 */
public interface TCCFenceStore {

    /**
     * Query tcc fence do.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the tcc fence do
     */
    TCCFenceDO queryTCCFenceDO(DataSource dataSource, String xid, Long branchId);

    /**
     * Insert tcc fence do boolean.
     * @param tccFenceDO the tcc fence do
     * @return the boolean
     */
    boolean insertTCCFenceDO(DataSource dataSource, TCCFenceDO tccFenceDO);

    /**
     * Update tcc fence do boolean.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param newStatus the new status
     * @return the boolean
     */
    boolean updateTCCFenceDO(DataSource dataSource, String xid, Long branchId, int newStatus, int oldStatus);

    /**
     * Delete tcc fence do boolean.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    boolean deleteTCCFenceDO(DataSource dataSource, String xid, Long branchId);

    /**
     * Delete tcc fence by datetime.
     * @param datetime datetime
     * @return the boolean
     */
    boolean deleteTCCFenceDOByDate(DataSource dataSource, Timestamp datetime);

}
