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
package io.seata.core.message;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchCommitRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch commit request test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/23
 */
public class BranchCommitRequestTest {

    /**
     * To string test.
     *
     * @throws Exception the exception
     */
    @Test
    public void toStringTest() {
        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();

        branchCommitRequest.setXid("127.0.0.1:9999:39875642");
        branchCommitRequest.setBranchId(1);
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setResourceId("resource1");
        branchCommitRequest.setApplicationData("app1");

        Assertions.assertEquals("xid=127.0.0.1:9999:39875642,branchId=1,branchType=AT,"
            + "resourceId=resource1,applicationData=app1", branchCommitRequest.toString());

    }
}