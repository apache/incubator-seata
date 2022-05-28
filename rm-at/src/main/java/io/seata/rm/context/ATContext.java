/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.exception.ShouldNeverHappenException;

/**
 * @author funkye
 */
public class ATContext {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public final Map<String, Object> applicationData = new HashMap<>(2, 1.0001f);

    public String xid;

    public Long branchId;

    public boolean isGlobalLockRequire;


    /**
     * whether requires global lock in this connection
     *
     * @return
     */
    public boolean isGlobalLockRequire() {
        return isGlobalLockRequire;
    }

    /**
     * set whether requires global lock in this connection
     *
     * @param isGlobalLockRequire
     */
    public void setGlobalLockRequire(boolean isGlobalLockRequire) {
        this.isGlobalLockRequire = isGlobalLockRequire;
    }

    /**
     * In global transaction boolean.
     *
     * @return the boolean
     */
    public boolean inGlobalTransaction() {
        return xid != null;
    }

    /**
     * Is branch registered boolean.
     *
     * @return the boolean
     */
    public boolean isBranchRegistered() {
        return branchId != null;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public void bind(String xid) {
        if (xid == null) {
            throw new IllegalArgumentException("xid should not be null");
        }
        if (!inGlobalTransaction()) {
            setXid(xid);
        } else {
            if (!this.xid.equals(xid)) {
                throw new ShouldNeverHappenException(
                        String.format("bind xid: %s, while current xid: %s", xid, this.xid));
            }
        }
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public Long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
    /**
     * Reset.
     */
    public void reset() {
        this.reset(null);
    }

    /**
     * Reset.
     *
     * @param xid the xid
     */
    public void reset(String xid) {
        this.xid = xid;
        branchId = null;
        this.isGlobalLockRequire = false;
        applicationData.clear();
    }

}
