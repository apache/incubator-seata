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
package io.seata.server.console.impl.file;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.store.db.vo.GlobalSessionVO;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.result.SingleResult;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Global Session File ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("'${seata.store.mode}'.equals('file')")
public class GlobalSessionFileServiceImpl implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> queryAll(String applicationId, boolean withBranch) {
        throw new NotSupportYetException();
    }

    @Override
    public PageResult<GlobalSessionVO> queryByStatus(String applicationId, Integer status, boolean withBranch) {
        throw new NotSupportYetException();
    }

    @Override
    public SingleResult<GlobalSessionVO> queryByXid(String xid, boolean withBranch) {
        throw new NotSupportYetException();
    }

}
