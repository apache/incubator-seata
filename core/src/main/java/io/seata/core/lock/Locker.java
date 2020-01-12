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
package io.seata.core.lock;

import java.util.List;

/**
 * The interface Locker.
 *
 * @author zhangsen
 */
public interface Locker {

    /**
     * Acquire lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean acquireLock(List<RowLock> rowLock) ;

    /**
     * Un lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean releaseLock(List<RowLock> rowLock);

    /**
     * Un lock boolean.
     *
     * @param xid the xid
     * @param branchId the branchId
     * @return the boolean
     */
    boolean releaseLock(String xid, Long branchId);

    /**
     * Un lock boolean.
     *
     * @param xid the xid
     * @param branchIds the branchIds
     * @return the boolean
     */
    boolean releaseLock(String xid, List<Long> branchIds);

    /**
     * Is lockable boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean isLockable(List<RowLock> rowLock);

    /**
     * Clean all locks boolean.
     *
     * @return the boolean
     */
    void cleanAllLocks();
}

