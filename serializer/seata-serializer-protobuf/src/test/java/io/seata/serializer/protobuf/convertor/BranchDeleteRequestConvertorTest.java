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
package io.seata.serializer.protobuf.convertor;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchDeleteRequest;
import io.seata.serializer.protobuf.generated.BranchDeleteRequestProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDeleteRequestConvertorTest {
    @Test
    public void convert2Proto() {
        BranchDeleteRequest branchDeleteRequest = new BranchDeleteRequest();
        branchDeleteRequest.setBranchType(BranchType.AT);
        branchDeleteRequest.setXid("xid");
        branchDeleteRequest.setResourceId("resourceId");
        branchDeleteRequest.setBranchId(123);

        BranchDeleteRequestConvertor branchDeleteRequestConvertor = new BranchDeleteRequestConvertor();
        BranchDeleteRequestProto proto = branchDeleteRequestConvertor.convert2Proto(
                branchDeleteRequest);
        BranchDeleteRequest realRequest = branchDeleteRequestConvertor.convert2Model(proto);

        assertThat(realRequest.getTypeCode()).isEqualTo(branchDeleteRequest.getTypeCode());
        assertThat(realRequest.getBranchType()).isEqualTo(branchDeleteRequest.getBranchType());
        assertThat(realRequest.getXid()).isEqualTo(branchDeleteRequest.getXid());
        assertThat(realRequest.getResourceId()).isEqualTo(branchDeleteRequest.getResourceId());
        assertThat(realRequest.getBranchId()).isEqualTo(branchDeleteRequest.getBranchId());

    }
}
