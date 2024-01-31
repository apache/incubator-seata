/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BranchRegisterRequestConvertorTest {

    @Test
    public void convert2Proto() {

        BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();
        branchRegisterRequest.setApplicationData("data");
        branchRegisterRequest.setBranchType(BranchType.AT);
        branchRegisterRequest.setLockKey("localKey");
        branchRegisterRequest.setResourceId("resourceId");
        branchRegisterRequest.setXid("xid");

        BranchRegisterRequestConvertor convertor = new BranchRegisterRequestConvertor();
        BranchRegisterRequestProto proto = convertor.convert2Proto(
            branchRegisterRequest);
        BranchRegisterRequest real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(branchRegisterRequest.getTypeCode());
        assertThat(real.getApplicationData()).isEqualTo(branchRegisterRequest.getApplicationData());
        assertThat(real.getXid()).isEqualTo(branchRegisterRequest.getXid());
        assertThat(real.getBranchType()).isEqualTo(branchRegisterRequest.getBranchType());
        assertThat(real.getLockKey()).isEqualTo(branchRegisterRequest.getLockKey());
        assertThat(real.getResourceId()).isEqualTo(branchRegisterRequest.getResourceId());
    }
}
