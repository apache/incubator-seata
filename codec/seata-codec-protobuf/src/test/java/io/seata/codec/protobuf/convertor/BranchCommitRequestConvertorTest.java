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

import io.seata.codec.protobuf.generated.BranchCommitRequestProto;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class BranchCommitRequestConvertorTest {

    @Test
    public void convert2Proto() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(123);
        branchCommitRequest.setApplicationData("app");

        BranchCommitRequestConvertor branchCommitRequestConvertor = new BranchCommitRequestConvertor();
        BranchCommitRequestProto proto = branchCommitRequestConvertor.convert2Proto(
            branchCommitRequest);
        BranchCommitRequest realRequest = branchCommitRequestConvertor.convert2Model(proto);

        assertThat(realRequest.getTypeCode()).isEqualTo(branchCommitRequest.getTypeCode());
        assertThat(realRequest.getBranchType()).isEqualTo(branchCommitRequest.getBranchType());
        assertThat(realRequest.getXid()).isEqualTo(branchCommitRequest.getXid());
        assertThat(realRequest.getResourceId()).isEqualTo(branchCommitRequest.getResourceId());
        assertThat(realRequest.getBranchId()).isEqualTo(branchCommitRequest.getBranchId());
        assertThat(realRequest.getApplicationData()).isEqualTo(branchCommitRequest.getApplicationData());

    }
}