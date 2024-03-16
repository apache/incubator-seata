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
package org.apache.seata.serializer.seata.protocol.transaction;

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.serializer.seata.SeataSerializer;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * The type Global lock query request codec test.
 *
 */
public class GlobalLockQueryRequestSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalLockQueryRequest globalLockQueryRequest = new GlobalLockQueryRequest();
        globalLockQueryRequest.setApplicationData("aaaa");
        globalLockQueryRequest.setBranchType(BranchType.TCC);
        globalLockQueryRequest.setLockKey("a:1,b,2");
        globalLockQueryRequest.setXid("aaa");
        globalLockQueryRequest.setResourceId("1s");

        byte[] bytes = seataSerializer.serialize(globalLockQueryRequest);

        GlobalLockQueryRequest globalLockQueryRequest2 = seataSerializer.deserialize(bytes);

        assertThat(globalLockQueryRequest2.getApplicationData()).isEqualTo(globalLockQueryRequest.getApplicationData());
        assertThat(globalLockQueryRequest2.getBranchType()).isEqualTo(globalLockQueryRequest.getBranchType());
        assertThat(globalLockQueryRequest2.getLockKey()).isEqualTo(globalLockQueryRequest.getLockKey());
        assertThat(globalLockQueryRequest2.getResourceId()).isEqualTo(globalLockQueryRequest.getResourceId());
        assertThat(globalLockQueryRequest2.getXid()).isEqualTo(globalLockQueryRequest.getXid());
    }

}
