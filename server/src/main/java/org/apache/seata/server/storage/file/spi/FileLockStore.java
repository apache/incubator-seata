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
package org.apache.seata.server.storage.file.spi;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

/**
 * Lock storage operations owned by a file-mode runtime.
 */
public interface FileLockStore {

    /**
     * Gets a locker for the supplied branch session.
     *
     * @param branchSession the branch session
     * @return the locker
     */
    Locker getLocker(BranchSession branchSession);

    /**
     * Releases locks owned by a branch session.
     *
     * @param branchSession the branch session
     * @return whether the locks were released
     * @throws TransactionException if the locks cannot be released
     */
    boolean releaseBranchLock(BranchSession branchSession) throws TransactionException;

    /**
     * Releases locks owned by a global session.
     *
     * @param globalSession the global session
     * @return whether the locks were released
     * @throws TransactionException if the locks cannot be released
     */
    boolean releaseGlobalLock(GlobalSession globalSession) throws TransactionException;
}
