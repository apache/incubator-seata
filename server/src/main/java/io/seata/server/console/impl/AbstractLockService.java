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

import io.seata.common.util.StringUtils;
import io.seata.console.result.SingleResult;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.service.GlobalLockService;

public abstract class AbstractLockService extends AbstractService implements GlobalLockService {

    @Override
    public SingleResult<Boolean> check(String xid, String branchId) {
        try {
            commonCheckAndGetGlobalStatus(xid, branchId);
        } catch (IllegalArgumentException e) {
            return SingleResult.success(Boolean.FALSE);
        }
        return SingleResult.success(Boolean.TRUE);
    }

    protected void checkDeleteLock(GlobalLockParam param) {
        commonCheck(param.getXid(), param.getBranchId());
        if (StringUtils.isBlank(param.getTableName()) || StringUtils.isBlank(param.getPk())
                || StringUtils.isBlank(param.getResourceId())) {
            throw new IllegalArgumentException("tableName or resourceId or pk can not be empty");
        }
    }
}
