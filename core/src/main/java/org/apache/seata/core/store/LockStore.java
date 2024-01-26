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
package org.apache.seata.core.store;

import java.util.List;
import org.apache.seata.core.model.LockStatus;

/**
 * The interface Lock store.
 *
 */
public interface LockStore {

    /**
     * Acquire lock boolean.
     *
     * @param lockDO the lock do
     * @return the boolean
     */
    boolean acquireLock(LockDO lockDO);


    /**
     * Acquire lock boolean.
     *
     * @param lockDOs the lock d os
     * @return the boolean
     */
    boolean acquireLock(List<LockDO> lockDOs);

    /**
     * Acquire lock boolean.
     *
     * @param lockDOs the lock d os
     * @param autoCommit the auto commit
     * @param skipCheckLock whether skip check lock or not
     * @return the boolean
     */
    boolean acquireLock(List<LockDO> lockDOs, boolean autoCommit, boolean skipCheckLock);

    /**
     * Un lock boolean.
     *
     * @param lockDO the lock do
     * @return the boolean
     */
    boolean unLock(LockDO lockDO);

    /**
     * Un lock boolean.
     *
     * @param lockDOs the lock d os
     * @return the boolean
     */
    boolean unLock(List<LockDO> lockDOs);

    boolean unLock(String xid);

    boolean unLock(Long branchId);

    /**
     * Is lockable boolean.
     *
     * @param lockDOs the lock do
     * @return the boolean
     */
    boolean isLockable(List<LockDO> lockDOs);

    /**
     * update lock status .
     *
     * @param xid the xid
     * @param lockStatus the lock status
     *
     */
    void updateLockStatus(String xid, LockStatus lockStatus);

}
