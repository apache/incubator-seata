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
package org.apache.seata.serializer.fory;

import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ForySerializerTest {
    private static ForySerializer forySerializer;

    @BeforeAll
    public static void before() {
        forySerializer = new ForySerializer();
    }

    @Test
    public void testBranchCommitRequest() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(20190809);
        branchCommitRequest.setApplicationData("app");

        byte[] bytes = forySerializer.serialize(branchCommitRequest);
        BranchCommitRequest t = forySerializer.deserialize(bytes);

        assertThat(t.getTypeCode()).isEqualTo(branchCommitRequest.getTypeCode());
        assertThat(t.getBranchType()).isEqualTo(branchCommitRequest.getBranchType());
        assertThat(t.getXid()).isEqualTo(branchCommitRequest.getXid());
        assertThat(t.getResourceId()).isEqualTo(branchCommitRequest.getResourceId());
        assertThat(t.getBranchId()).isEqualTo(branchCommitRequest.getBranchId());
        assertThat(t.getApplicationData()).isEqualTo(branchCommitRequest.getApplicationData());
    }

    @Test
    public void testBranchCommitResponse() {

        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchCommitResponse.setBranchId(20190809);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchCommitResponse.setMsg("20190809");
        branchCommitResponse.setXid("20190809");
        branchCommitResponse.setResultCode(ResultCode.Failed);

        byte[] bytes = forySerializer.serialize(branchCommitResponse);
        BranchCommitResponse t = forySerializer.deserialize(bytes);

        assertThat(t.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(t.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(t.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(t.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(t.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());
    }

    @Test
    public void testLoadSerializerWithType() {
        Serializer serializerFury = SerializerServiceLoader.load(SerializerType.FURY);
        Assertions.assertNotNull(serializerFury, "FURY Serializer should be available");

        Serializer serializerFory = SerializerServiceLoader.load(SerializerType.FORY);
        Assertions.assertNotNull(serializerFory, "FORY Serializer should be available");

        Assertions.assertEquals(serializerFury, serializerFory, "FORY Serializer should be equal FURY");
    }

    @Test
    public void testLoadSerializerWithVersion() {
        Serializer serializerFury = SerializerServiceLoader.load(SerializerType.FURY, (byte) 0x01);
        Assertions.assertNotNull(serializerFury, "FURY Serializer should be available");

        Serializer serializerFory = SerializerServiceLoader.load(SerializerType.FORY, (byte) 0x01);
        Assertions.assertNotNull(serializerFory, "FORY Serializer should be available");

        Assertions.assertEquals(serializerFury, serializerFory, "FORY Serializer should be equal FURY");
    }
}
