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
package io.seata.server.console.impl.db;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Global Session DataBase ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class GlobalSessionDBServiceImpl implements GlobalSessionService {

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        throw new NotSupportYetException();
    }

}
