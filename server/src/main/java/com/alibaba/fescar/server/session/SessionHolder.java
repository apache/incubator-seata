/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.server.session;

import java.io.IOException;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.core.exception.TransactionException;

public class SessionHolder {

    private static final String ROOT_SESSION_MANAGER_NAME = "root.data";
    private static final String ASYNC_COMMITTING_SESSION_MANAGER_NAME = "async.commit.data";
    private static final String RETRY_COMMITTING_SESSION_MANAGER_NAME = "retry.commit.data";
    private static final String RETRY_ROLLBACKING_SESSION_MANAGER_NAME = "retry.rollback.data";

    private static SessionManager ROOT_SESSION_MANAGER;

    private static SessionManager ASYNC_COMMITTING_SESSION_MANAGER;

    private static SessionManager RETRY_COMMITTING_SESSION_MANAGER;

    private static SessionManager RETRY_ROLLBACKING_SESSION_MANAGER;

    public static void init(String sessionStorePath) throws IOException {
        if (sessionStorePath == null) {
            ROOT_SESSION_MANAGER = new DefaultSessionManager(ROOT_SESSION_MANAGER_NAME);
            ASYNC_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(ASYNC_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(RETRY_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_ROLLBACKING_SESSION_MANAGER = new DefaultSessionManager(RETRY_ROLLBACKING_SESSION_MANAGER_NAME);
        } else {
            if (!sessionStorePath.endsWith("/")) {
                sessionStorePath = sessionStorePath + "/";
            }
            ROOT_SESSION_MANAGER = new FileBasedSessionManager(ROOT_SESSION_MANAGER_NAME, sessionStorePath);
            ASYNC_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(ASYNC_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(RETRY_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_ROLLBACKING_SESSION_MANAGER = new DefaultSessionManager(RETRY_ROLLBACKING_SESSION_MANAGER_NAME);

        }

    }

    public static final SessionManager getRootSessionManager() {
        if (ROOT_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ROOT_SESSION_MANAGER;
    }
    public static final SessionManager getAsyncCommittingSessionManager() {
        if (ASYNC_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ASYNC_COMMITTING_SESSION_MANAGER;
    }
    public static final SessionManager getRetryCommittingSessionManager() {
        if (RETRY_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_COMMITTING_SESSION_MANAGER;
    }
    public static final SessionManager getRetryRollbackingSessionManager() {
        if (RETRY_ROLLBACKING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_ROLLBACKING_SESSION_MANAGER;
    }

    public static GlobalSession findGlobalSession(Long transactionId) throws TransactionException {
        return getRootSessionManager().findGlobalSession(transactionId);
    }
}
