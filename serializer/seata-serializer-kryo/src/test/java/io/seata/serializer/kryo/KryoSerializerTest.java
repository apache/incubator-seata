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
package io.seata.serializer.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jsbxyyx
 */
public class KryoSerializerTest {

    private static KryoSerializer kryoCodec;

    @BeforeAll
    public static void before() {
        kryoCodec = new KryoSerializer();
    }

    /**
     * 测试jdk版本对内置对象序列化的兼容性
     */
    @Test
    public void testSerializerFactory() {
        KryoSerializerFactory factory = KryoSerializerFactory.getInstance();
        KryoInnerSerializer kryoInnerSerializer = factory.get();
        Kryo kryo = kryoInnerSerializer.getKryo();
        assertThat(kryo).isNotNull();
        factory.returnKryo(kryoInnerSerializer);
    }

    @Test
    public void testBranchCommitRequest() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(20190809);
        branchCommitRequest.setApplicationData("app");

        byte[] bytes = kryoCodec.serialize(branchCommitRequest);
        BranchCommitRequest t = kryoCodec.deserialize(bytes);

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

        byte[] bytes = kryoCodec.serialize(branchCommitResponse);
        BranchCommitResponse t = kryoCodec.deserialize(bytes);

        assertThat(t.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(t.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(t.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(t.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(t.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());

    }

    @Test
    public void testKryoBasic() {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        //kryo.register(HashMap.class);

        long beginMills=System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            Map<String, String> map = new HashMap<>();
            map.put(String.valueOf(i), "test");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Output output = new Output(outputStream);
            kryo.writeClassAndObject(output, map);
            output.close();
            byte[] outByte = outputStream.toByteArray();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outByte);
            Input input = new Input(inputStream);
            input.close();
            Map result = (HashMap)kryo.readClassAndObject(input);
            assertThat(result).isEqualTo(map);
        }
        //System.out.println(System.currentTimeMillis()-beginMills);
    }

}
