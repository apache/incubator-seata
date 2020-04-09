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
package io.seata.server.transaction.xa;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.coordinator.AbstractCore;

/**
 * The type XA core.
 *
 * @author sharajava
 */
public class XACore extends AbstractCore {

    public XACore(ServerMessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.XA;
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        super.branchReport(branchType, xid, branchId, status, applicationData);
        if (BranchStatus.PhaseOne_Failed == status) {

        }
    }
}
