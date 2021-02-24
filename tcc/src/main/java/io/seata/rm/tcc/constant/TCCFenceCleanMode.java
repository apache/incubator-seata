package io.seata.rm.tcc.constant;

/**
 * TCC Fence clean mode
 *
 * @author cebbank
 */
public enum TCCFenceCleanMode {

    /**
     * Close auto clean task
     */
    Close,
    /**
     * Default clean mode.
     */
    Default,
    /**
     * Clean by days
     */
    Day,
    /**
     * Clean by hours
     */
    Hour;
}
