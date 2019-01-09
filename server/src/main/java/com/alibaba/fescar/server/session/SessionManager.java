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

import java.util.Collection;
import java.util.List;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.GlobalStatus;


public interface SessionManager extends SessionLifecycleListener {

    void addGlobalSession(GlobalSession session) throws TransactionException;

    GlobalSession findGlobalSession(Long transactionId) throws TransactionException;

    void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException;

    void removeGlobalSession(GlobalSession session) throws TransactionException;

    void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException;

    void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    Collection<GlobalSession> allSessions();

    List<GlobalSession> findGlobalSessions(SessionCondition condition);


}
