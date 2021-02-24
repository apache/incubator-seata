package io.seata.rm.tcc.constant;

/**
 * TCC Fence Constant
 *
 * @author cebbank
 */
public class TCCFenceConstant {

    /**
     * PHASE 1: The Commit tried.
     */
    public static final int STATUS_TRIED = 1;

    /**
     * PHASE 2: The Committed.
     */
    public static final int STATUS_COMMITTED = 2;

    /**
     * PHASE 2: The Rollbacked.
     */
    public static final int STATUS_ROLLBACKED = 3;

    /**
     * Suspended status.
     */
    public static final int STATUS_SUSPENDED = 4;

    /**
     * Clean up the tcc fence log a few days ago by default
     */
    public static final int DEFAULT_CLEAN_DAY = 1;

    /**
     * Clean up the tcc fence log a few hours ago by default
     */
    public static final int DEFAULT_CLEAN_HOUR = 1;

    /**
     * Default tcc fence log table name
     */
    public static final String DEFAULT_LOG_TABLE_NAME = "tcc_fence_log";

}
