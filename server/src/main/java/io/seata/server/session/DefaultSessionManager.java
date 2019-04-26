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
package io.seata.server.session;

import java.util.List;

import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionWriteStore;

/**
 * The type Default session manager.
 *
 * @author sharajava
 */
public class DefaultSessionManager extends AbstractSessionManager {

    /**
     * Instantiates a new Default session manager.
     *
     * @param name the name
     */
    public DefaultSessionManager(String name) {
        super(name);
        transactionStoreManager = new TransactionStoreManager() {
            @Override
            public boolean writeSession(LogOperation logOperation, SessionStorable session) {
                return true;
            }

            @Override
            public void shutdown() {

            }

            @Override
            public List<TransactionWriteStore> readWriteStoreFromFile(int readSize, boolean isHistory) {
                return null;
            }

            @Override
            public boolean hasRemaining(boolean isHistory) {
                return false;
            }
        };
    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }
}
