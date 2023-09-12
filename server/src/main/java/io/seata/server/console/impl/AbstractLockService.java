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
package io.seata.server.console.impl;

import io.seata.common.exception.FrameworkException;
import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.core.exception.TransactionException;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLockService extends AbstractService implements GlobalLockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockService.class);

    @Override
    public SingleResult<Void> deleteLock(String xid, String branchId) {
        LOGGER.debug("start to delete global lock , xid:{}, branchId:{}", xid, branchId);
        commonCheck(xid, branchId);
        try {
            // global lock exist,the branchSession may be not exist in DB/Redis
            BranchSession branchSession = new BranchSession();
            branchSession.setBranchId(Long.parseLong(branchId));
            branchSession.setXid(xid);
            return lockManager.releaseLock(branchSession) ? SingleResult.success() : SingleResult.failure(Code.ERROR);
        } catch (TransactionException e) {
            LOGGER.error("Release lock fail, xid:{}, branchId:{}", xid, branchId, e);
            throw new FrameworkException(e);
        } catch (Exception e) {
            LOGGER.error("Release lock fail, xid:{}, branchId:{}", xid, branchId, e);
            throw e;
        }
    }
}
