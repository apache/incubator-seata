/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * XA-Xid builder.
 *
 */
public class XAXidBuilder {

    private XAXidBuilder() {}

    public static final XAXid build(String xid, long branchId) {
        return build(xid, branchId, null);
    }

    /**
     * Build a XAXid from the given Seata global transaction xid and branch id.
     * <p>
     * For databases that reject a new xa start with XAER_RMERR while an in-doubt branch of the
     * same gtrid exists (e.g. DM), the returned XAXid uses a per-branch-qualified gtrid
     * ({@code xid + "-" + branchId}) with a dedicated format id, so that every branch of one
     * Seata global transaction owns a distinct gtrid and can be started, prepared and decided
     * independently by the database. If the readable form would exceed the X/Open maximum gtrid
     * length, a bounded (md5-hex based) encoding of the xid is used instead, so the build never
     * fails for a valid Seata xid. The rebuild on the phase-2 path is deterministic, since the
     * gtrid is derived from the xid and the branch id only, both of which are carried by the
     * branch commit/rollback requests of the TC.
     * <p>
     * For all other databases (or if dbType is null) the legacy encoding (gtrid = xid) is used,
     * which stays byte-to-byte identical to previous versions.
     *
     * @param xid the Seata global transaction xid
     * @param branchId the branch id
     * @param dbType the database type of the resource this XAXid is used against, may be null
     * @return the XAXid
     */
    public static final XAXid build(String xid, long branchId, String dbType) {
        if (JdbcConstants.DM.equalsIgnoreCase(dbType)) {
            return new XABranchXid(xid, branchId, XABranchXid.SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID);
        }
        return new XABranchXid(xid, branchId);
    }

    /**
     * Rebuild a XAXid from raw XA bytes.
     * <p>
     * Note: only the legacy encoding (format id {@code 9752}, gtrid = xid) can be reconstructed
     * from raw bytes. Per-branch-qualified gtrids must be rebuilt via
     * {@link #build(String, long, String)}.
     *
     * @param globalTransactionId the raw global transaction id bytes
     * @param branchQualifier the raw branch qualifier bytes
     * @return the XAXid
     */
    public static final XAXid build(byte[] globalTransactionId, byte[] branchQualifier) {
        return new XABranchXid(globalTransactionId, branchQualifier);
    }
}
