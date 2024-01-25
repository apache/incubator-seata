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
package org.apache.seata.core.lock;

import java.util.List;
import org.apache.seata.core.model.LockStatus;

/**
 * The type Local db locker.
 *
 */
public class LocalDBLocker extends AbstractLocker {

    @Override
    public boolean acquireLock(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLock, boolean autoCommit, boolean skipCheckLock) {
        return false;
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public boolean isLockable(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
    }

}
