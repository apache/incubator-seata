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
package org.apache.seata.benchmark.baseline.serializer;

import org.apache.seata.common.XID;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.UUIDGenerator;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(value = Scope.Benchmark)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SerializerBenchMark {

    @Param({"SEATA", "PROTOBUF", "KRYO", "HESSIAN"})
    private String serializerType;

    @Param({"GlobalBeginRequest", "GlobalBeginResponse", "GlobalCommitRequest", "GlobalCommitResponse"})
    private String type;

    private Object serializerObject;

    private byte[] deserializerByteArray;

    private Serializer serializer;

    @Setup(Level.Trial)
    public void setup() {
        serializer = SerializerServiceLoader.load(SerializerType.getByName(serializerType));

        switch (type) {
            case "GlobalBeginRequest":
                GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
                globalBeginRequest.setTimeout(10);
                globalBeginRequest.setTransactionName("transactionName");
                serializerObject = globalBeginRequest;
                break;
            case "GlobalBeginResponse":
                GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
                globalBeginResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotActive);
                globalBeginResponse.setExtraData("{\"key\",\"value\"}");
                globalBeginResponse.setXid(XID.generateXID(UUIDGenerator.generateUUID()));
                globalBeginResponse.setResultCode(ResultCode.Failed);
                globalBeginResponse.setMsg("msg");
                serializerObject = globalBeginResponse;
                break;
            case "GlobalCommitRequest":
                GlobalCommitRequest globalCommitRequest = new GlobalCommitRequest();
                globalCommitRequest.setExtraData("{\"key\",\"value\"}");
                globalCommitRequest.setXid(XID.generateXID(UUIDGenerator.generateUUID()));
                serializerObject = globalCommitRequest;
                break;
            case "GlobalCommitResponse":
                GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();
                globalCommitResponse.setGlobalStatus(GlobalStatus.AsyncCommitting);
                globalCommitResponse.setMsg("msg");
                globalCommitResponse.setResultCode(ResultCode.Failed);
                globalCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionStatusInvalid);
                serializerObject = globalCommitResponse;
                break;
        }
        deserializerByteArray = serializer.serialize(serializerObject);
    }

    @Benchmark
    public void testSerialize() {
        serializer.serialize(serializerObject);
    }

    @Benchmark
    public void testDeserialize() {
        serializer.deserialize(deserializerByteArray);
    }
}
