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

import javax.annotation.Nonnull;

/**
 * Holder for suspended resources to support propagation or nested logic.
 * Used by {@code suspend} and {@code resume}
 *
 * @author wangzhongxiang
 * @author wang.liang
 */
public class SuspendedResourcesHolder {

    /**
     * The xid
     */
    private String xid;

    public SuspendedResourcesHolder(String xid) {
        if (xid == null) {
            throw new IllegalArgumentException("xid must be not null");
        }
        this.xid = xid;
    }

    @Nonnull
    public String getXid() {
        return xid;
    }
}
