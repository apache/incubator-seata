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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.session.SessionManager;

@LoadLevel(name = "session-holder-fault")
public final class SessionHolderFaultFileStoreProvider implements FileStoreProvider {

    public enum FailurePoint {
        NONE,
        OPEN,
        LOCK_STORE,
        RECOVERY,
        BACKGROUND
    }

    private static FailurePoint failurePoint = FailurePoint.NONE;
    private static RuntimeException startupFailure;
    private static RuntimeException closeFailure;
    private static SessionManager sessionManager;
    private static FileLockStore lockStore;
    private static FileStoreContext lastContext;
    private static int closeCount;

    public static void configure(
            FailurePoint point,
            RuntimeException configuredStartupFailure,
            RuntimeException configuredCloseFailure,
            SessionManager configuredSessionManager,
            FileLockStore configuredLockStore) {
        failurePoint = point;
        startupFailure = configuredStartupFailure;
        closeFailure = configuredCloseFailure;
        sessionManager = configuredSessionManager;
        lockStore = configuredLockStore;
        lastContext = null;
        closeCount = 0;
    }

    public static int closeCount() {
        return closeCount;
    }

    public static FileStoreContext lastContext() {
        return lastContext;
    }

    public static void reset() {
        configure(FailurePoint.NONE, null, null, null, null);
    }

    @Override
    public FileStoreRuntime open(FileStoreContext context) {
        lastContext = context;
        if (failurePoint == FailurePoint.OPEN) {
            throw startupFailure;
        }
        return new FileStoreRuntime() {
            private boolean closed;

            @Override
            public SessionManager sessionManager() {
                return sessionManager;
            }

            @Override
            public FileLockStore lockStore() {
                if (failurePoint == FailurePoint.LOCK_STORE) {
                    throw startupFailure;
                }
                return lockStore;
            }

            @Override
            public void recover(SessionRecoveryConsumer consumer) {
                if (failurePoint == FailurePoint.RECOVERY) {
                    throw startupFailure;
                }
                try {
                    consumer.accept(sessionManager.allSessions());
                } catch (TransactionException e) {
                    throw new StoreException(e, "recover test file store sessions failed");
                }
            }

            @Override
            public void startBackgroundServices() {
                if (failurePoint == FailurePoint.BACKGROUND) {
                    throw startupFailure;
                }
            }

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                closed = true;
                closeCount++;
                if (closeFailure != null) {
                    throw closeFailure;
                }
            }
        };
    }
}
