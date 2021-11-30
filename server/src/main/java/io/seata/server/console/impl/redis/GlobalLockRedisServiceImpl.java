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
package io.seata.server.console.impl.redis;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.store.db.vo.GlobalLockVO;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.service.GlobalLockService;

/**
 * Global Lock Redis Service Impl
 * @author: zhongxiang.wang
 */
@LoadLevel(name = "redis", scope = Scope.SINGLETON)
public class GlobalLockRedisServiceImpl implements GlobalLockService {

    @Override
    public PageResult<GlobalLockVO> queryByTable(String tableName) {
        throw new NotSupportYetException();
    }

    @Override
    public PageResult<GlobalLockVO> queryByXid(String xid) {
        throw new NotSupportYetException();
    }
}
