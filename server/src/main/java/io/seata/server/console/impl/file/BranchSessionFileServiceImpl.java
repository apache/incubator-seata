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

import io.seata.common.util.StringUtils;
import io.seata.server.console.impl.AbstractBranchService;
import io.seata.server.console.vo.BranchSessionVO;
import io.seata.console.result.PageResult;
import io.seata.server.console.service.BranchSessionService;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Branch Session File ServiceImpl
 *
 * @author zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'file'.equals('${sessionMode}')}")
public class BranchSessionFileServiceImpl extends AbstractBranchService implements BranchSessionService {

    @Override
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        if (StringUtils.isBlank(xid)) {
            throw new IllegalArgumentException("xid should not be blank");
        }
        List<BranchSessionVO> branchSessionVOList = new ArrayList<>(0);
        final Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();
        for (GlobalSession globalSession : allSessions) {
            if (globalSession.getXid().equals(xid)) {
                Set<BranchSessionVO> branchSessionVOS = SessionConverter.convertBranchSession(globalSession.getBranchSessions());
                branchSessionVOList = new ArrayList<>(branchSessionVOS);
                break;
            }
        }
        return PageResult.success(branchSessionVOList, branchSessionVOList.size(), 0, 0, 0);
    }
}
