package io.seata.rm.datasource.util;

import javax.transaction.xa.XAResource;

/**
 * @author PeppaO
 * @since 2023/3/16
 */
public interface SeataXAResource extends XAResource {
    // OracleXAResource Loosely Coupled Branches
    public static final int ORATRANSLOOSE = 65536;

}
