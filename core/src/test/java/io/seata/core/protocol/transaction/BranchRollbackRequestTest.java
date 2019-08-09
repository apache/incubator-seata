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
package io.seata.core.protocol.transaction;


import io.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author: jimin.jm@alibaba-inc.com
 * @date 2019/04/16
 */
public class BranchRollbackRequestTest {
    @Test
    public void toStringTest() {
        BranchRollbackRequest branchRollbackRequest = new BranchRollbackRequest();

        branchRollbackRequest.setXid("127.0.0.1:9999:39875642");
        branchRollbackRequest.setBranchId(1);
        branchRollbackRequest.setBranchType(BranchType.AT);
        branchRollbackRequest.setResourceId("resource1");
        branchRollbackRequest.setApplicationData("app1");

        Assertions.assertEquals("xid=127.0.0.1:9999:39875642,branchId=1,branchType=AT,"
            + "resourceId=resource1,applicationData=app1", branchRollbackRequest.toString());

    }

}