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
package io.seata.server.cluster.raft.snapshot.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.LockStatus;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionSnapshot {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSnapshot.class);

    private Map<byte[]/*global session*/, List<byte[]>/*branch sessions*/> globalsessions = new ConcurrentHashMap<>();

    public Map<byte[], List<byte[]>> getGlobalsessions() {
        return globalsessions;
    }

    public void setGlobalsessions(Map<byte[], List<byte[]>> globalsessions) {
        this.globalsessions = globalsessions;
    }

    public Map<String, GlobalSession> convert2GlobalSession() {
        Map<String, GlobalSession> sessionMap = new HashMap<>();
        globalsessions.forEach((globalSessionByte, branchSessionBytes) -> {
            GlobalSession globalSession = new GlobalSession();
            globalSession.decode(globalSessionByte);
            branchSessionBytes.forEach(branch -> {
                BranchSession branchSession = new BranchSession();
                branchSession.decode(branch);
                if (globalSession.isActive()) {
                    try {
                        branchSession.lock();
                    } catch (TransactionException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
                globalSession.add(branchSession);
            });
            if (GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                globalSession.getBranchSessions().parallelStream()
                    .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
            }
            sessionMap.put(globalSession.getXid(), globalSession);
        });
        return sessionMap;
    }

    public void convert2GlobalSessionByte(GlobalSession globalSession) {
        byte[] globalSessionByte = globalSession.encode();
        if (CollectionUtils.isEmpty(globalSession.getBranchSessions())) {
            globalsessions.put(globalSessionByte, Collections.emptyList());
        } else {
            globalsessions.put(globalSessionByte, globalSession.getBranchSessions().parallelStream()
                .map(branch -> branch.encode()).collect(Collectors.toList()));
        }
    }

}
