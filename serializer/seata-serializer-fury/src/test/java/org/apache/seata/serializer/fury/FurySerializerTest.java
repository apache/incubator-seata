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
package org.apache.seata.serializer.fury;

import org.apache.fory.Fory;
import org.apache.fury.Fury;
import org.apache.fury.config.CompatibleMode;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.serializer.Serializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.seata.test.TestUnSafeSerializer;

import static org.assertj.core.api.Assertions.assertThat;

public class FurySerializerTest {
    private static FurySerializer furySerializer;

    @BeforeAll
    public static void before() {
        furySerializer = new FurySerializer();
    }

    @Test
    public void testBranchCommitRequest() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(20190809);
        branchCommitRequest.setApplicationData("app");

        byte[] bytes = furySerializer.serialize(branchCommitRequest);
        BranchCommitRequest t = furySerializer.deserialize(bytes);

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

        byte[] bytes = furySerializer.serialize(branchCommitResponse);
        BranchCommitResponse t = furySerializer.deserialize(bytes);

        assertThat(t.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(t.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(t.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(t.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(t.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());
    }

    @Test
    public void testUnSafeDeserializer() {
        // Test deserialization of an object that is not in allow list
        TestUnSafeSerializer testUnSafeSerializer = new TestUnSafeSerializer();
        byte[] bytes = new TestSerializerFactory().get().serialize(testUnSafeSerializer);
        String className = null;
        try {
            furySerializer.deserialize(bytes);
        } catch (Exception e) {
            className = e.getClass().getSimpleName();
        }
        Assertions.assertEquals("DeserializationException", className);
    }

    @Test
    public void testUnSafeSerializer() {
        // Test serialization of an object that is not in allow list
        TestUnSafeSerializer testUnSafeSerializer = new TestUnSafeSerializer();
        String className = null;
        try {
            furySerializer.serialize(testUnSafeSerializer);
        } catch (Exception e) {
            className = e.getClass().getSimpleName();
        }
        Assertions.assertEquals("InsecureException", className);
    }

    private static class TestSerializerFactory {

        private final Serializer furyDelegate;

        public TestSerializerFactory() {
            this.furyDelegate = createFuryDelegate();
        }

        public Serializer get() {
            return furyDelegate;
        }

        private Serializer createFuryDelegate() {
            // First, try to load Apache Fory.
            try {
                Class.forName("org.apache.fory.Fury");
                return new ForyDelegate();
            } catch (ClassNotFoundException e) {
            }
            // If Fory is unavailable, fallback to Fury.
            try {
                Class.forName("org.apache.fury.Fury");
                return new FuryDelegate();
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Neither Apache Fory nor Apache Fury found in classpath", e);
            }
        }
    }

    private static class ForyDelegate implements Serializer {

        @Override
        public byte[] serialize(Object obj) {
            Fory fory = Fory.builder()
                    .requireClassRegistration(false)
                    .withRefTracking(true)
                    .withCompatibleMode(org.apache.fory.config.CompatibleMode.COMPATIBLE)
                    .build();
            return fory.serialize(obj);
        }

        @Override
        public Object deserialize(byte[] bytes) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FuryDelegate implements Serializer {

        @Override
        public byte[] serialize(Object obj) {
            Fury fury = Fury.builder()
                    .requireClassRegistration(false)
                    .withRefTracking(true)
                    .withCompatibleMode(CompatibleMode.COMPATIBLE)
                    .build();
            return fury.serialize(obj);
        }

        @Override
        public Object deserialize(byte[] bytes) {
            throw new UnsupportedOperationException();
        }
    }
}
