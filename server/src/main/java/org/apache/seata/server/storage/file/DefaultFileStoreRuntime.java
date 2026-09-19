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
package org.apache.seata.server.storage.file;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.storage.file.spi.SessionRecoveryConsumer;

public class DefaultFileStoreRuntime implements FileStoreRuntime {

    private final FileSessionManager sessionManager;
    private final FileLockStore lockStore;
    private State state = State.OPEN;
    private boolean recoveryComplete;

    DefaultFileStoreRuntime(FileSessionManager sessionManager, FileLockStore lockStore) {
        this.sessionManager = sessionManager;
        this.lockStore = lockStore;
    }

    @Override
    public SessionManager sessionManager() {
        return sessionManager;
    }

    @Override
    public FileLockStore lockStore() {
        return lockStore;
    }

    @Override
    public synchronized void recover(SessionRecoveryConsumer recoveryConsumer) {
        requireOpen();
        if (recoveryComplete) {
            throw new IllegalStateException("file store recovery already completed");
        }
        sessionManager.reload();
        try {
            recoveryConsumer.accept(sessionManager.allSessions());
            recoveryComplete = true;
        } catch (TransactionException e) {
            throw new StoreException(e, "recover default file store sessions failed");
        }
    }

    @Override
    public synchronized void startBackgroundServices() {
        requireOpen();
        if (!recoveryComplete) {
            throw new IllegalStateException("file store recovery must complete before background services start");
        }
        state = State.STARTING_BACKGROUND;
        state = State.RUNNING;
    }

    @Override
    public synchronized void close() {
        if (state == State.CLOSING || state == State.CLOSED) {
            return;
        }
        state = State.CLOSING;
        try {
            sessionManager.destroy();
        } finally {
            state = State.CLOSED;
        }
    }

    private void requireOpen() {
        if (state != State.OPEN) {
            throw new IllegalStateException("file store runtime is not open");
        }
    }

    private enum State {
        OPEN,
        STARTING_BACKGROUND,
        RUNNING,
        CLOSING,
        CLOSED
    }
}
