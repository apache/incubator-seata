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

import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.transaction.BranchReportRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch report request test.
 */
public class BranchReportRequestTest {

    /**
     * Test to string.
     */
    @Test
    public void testToString() {
        BranchReportRequest branchReportRequest = new BranchReportRequest();
        branchReportRequest.setXid("127.0.0.1:8091:1249853");
        branchReportRequest.setBranchId(3);
        branchReportRequest.setResourceId("resource003");
        branchReportRequest.setStatus(BranchStatus.PhaseOne_Timeout);
        branchReportRequest.setApplicationData("test app data");
        Assertions.assertEquals(
            "xid=127.0.0.1:8091:1249853,branchId=3,resourceId=resource003,status=PhaseOne_Timeout,"
                + "applicationData=test app"
                + " data",
            branchReportRequest.toString());
    }

}