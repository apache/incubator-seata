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
package io.seata.server.console.service;

import io.seata.core.store.db.vo.GlobalSessionVO;
import io.seata.server.console.result.PageResult;

/**
 * Global session service
 * @author wangzhongxiang
 */
public interface GlobalSessionService {

    /**
     * Query all global session
     * @return
     */
    PageResult<GlobalSessionVO> queryAll();

    /**
     * Query global session by status
     * @param status the session status
     * @return
     */
    PageResult<GlobalSessionVO> queryByStatus(Integer status);

    /**
     * Query by xid
     * @param xid the xid
     * @return
     */
    GlobalSessionVO queryByXid(String xid);

}
