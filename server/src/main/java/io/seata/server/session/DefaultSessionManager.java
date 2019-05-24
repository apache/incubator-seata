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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;

/**
 * The type Default session manager, store session data in memory.
 *
 * @author sharajava
 */
@LoadLevel(name = "default")
public class DefaultSessionManager extends AbstractSessionManager {

    /**
     * The Session map.
     */
    protected Map<String, GlobalSession> sessionMap = new ConcurrentHashMap<String, GlobalSession>();

    /**
     * Instantiates a new Default session manager.
     *
     * @param name the name
     */
    public DefaultSessionManager(String name) {
        super(name);
        transactionStoreManager = new AbstractTransactionStoreManager() {
            @Override
            public boolean writeSession(LogOperation logOperation, SessionStorable session) {
                return true;
            }
        };
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        super.addGlobalSession(session);
        sessionMap.put(session.getXid(), session);
    }

    @Override
    public GlobalSession findGlobalSession(String xid)  {
        return sessionMap.get(xid);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        super.removeGlobalSession(session);
        sessionMap.remove(session.getXid());
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return sessionMap.values();
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        List<GlobalSession> found = new ArrayList<>();
        for (GlobalSession globalSession : sessionMap.values()) {
            if (System.currentTimeMillis() - globalSession.getBeginTime() > condition.getOverTimeAliveMills()) {
                found.add(globalSession);
            }
        }
        return found;
    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }
}
