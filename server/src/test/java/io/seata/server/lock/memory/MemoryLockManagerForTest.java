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
package io.seata.server.lock.memory;

import io.seata.core.lock.Locker;
import io.seata.server.lock.DefaultLockManager;
import io.seata.server.session.BranchSession;

/**
 * @author zhangsen
 * @data 2019-05-16
 */
public class MemoryLockManagerForTest extends DefaultLockManager {

    @Override
    protected Locker getLocker(BranchSession branchSession) {
        return new MemoryLocker(branchSession);
    }
}
