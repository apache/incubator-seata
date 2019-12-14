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
package io.seata.codec.protobuf.convertor;

import io.seata.codec.protobuf.generated.BranchReportRequestProto;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchReportRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class BranchReportRequestConvertorTest {

    @Test
    public void convert2Proto() {

        BranchReportRequest branchReportRequest = new BranchReportRequest();

        branchReportRequest.setApplicationData("data");
        branchReportRequest.setBranchId(123);
        branchReportRequest.setResourceId("resourceId");
        branchReportRequest.setXid("xid");
        branchReportRequest.setBranchType(
            BranchType.AT);
        branchReportRequest.setStatus(BranchStatus.PhaseOne_Done);
        BranchReportRequestConvertor convertor = new BranchReportRequestConvertor();
        BranchReportRequestProto proto = convertor.convert2Proto(branchReportRequest);
        BranchReportRequest real = convertor.convert2Model(proto);

        assertThat(real.getBranchType()).isEqualTo(branchReportRequest.getBranchType());
        assertThat(real.getXid()).isEqualTo(branchReportRequest.getXid());
        assertThat(real.getResourceId()).isEqualTo(branchReportRequest.getResourceId());
        assertThat(real.getBranchId()).isEqualTo(branchReportRequest.getBranchId());
        assertThat(real.getApplicationData()).isEqualTo(branchReportRequest.getApplicationData());
        assertThat(real.getStatus()).isEqualTo(branchReportRequest.getStatus());

    }
}