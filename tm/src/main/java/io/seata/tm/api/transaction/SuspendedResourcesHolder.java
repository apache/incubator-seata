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
package io.seata.tm.api.transaction;

import io.seata.core.model.BranchType;

/**
 * Holder for suspended resources.
 * Used by {@code suspend} and {@code resume}.
 *
 * @author wangzhongxiang
 */
public class SuspendedResourcesHolder {

    /**The xid*/
    private String xid;

    /**The branchType*/
    private BranchType branchType;

    public SuspendedResourcesHolder() {
    }

    public SuspendedResourcesHolder(String xid, BranchType branchType) {
        this.xid = xid;
        this.branchType = branchType;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }
}
