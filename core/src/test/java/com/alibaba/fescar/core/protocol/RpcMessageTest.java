/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.protocol;

import com.alibaba.fastjson.JSON;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Rpc message test.
 *
 * @author guoyao
 * @date 2019 /3/2
 */
public class RpcMessageTest {

    private final boolean ASYNC_FIELD = false;
    private final String BODY_FIELD = "test_body";
    private final boolean HEART_BEAT_FIELD = true;
    private final boolean REQUEST_FIELD = false;
    private final long ID_FIELD = 100L;

    /**
     * Test field get set from json.
     */
    @Test
    public void testFieldGetSetFromJson() {
        String fromJson = "{\n" +
            "\t\"async\":" + ASYNC_FIELD + ",\n" +
            "\t\"body\":\"" + BODY_FIELD + "\",\n" +
            "\t\"heartbeat\":" + HEART_BEAT_FIELD + ",\n" +
            "\t\"id\":" + ID_FIELD + ",\n" +
            "\t\"request\":" + REQUEST_FIELD + "\n" +
            "}";
        RpcMessage fromJsonMessage = JSON.parseObject(fromJson, RpcMessage.class);
        assertThat(fromJsonMessage.isAsync()).isEqualTo(ASYNC_FIELD);
        assertThat(fromJsonMessage.isHeartbeat()).isEqualTo(HEART_BEAT_FIELD);
        assertThat(fromJsonMessage.isRequest()).isEqualTo(REQUEST_FIELD);
        assertThat(fromJsonMessage.getBody()).isEqualTo(BODY_FIELD);
        assertThat(fromJsonMessage.getId()).isEqualTo(ID_FIELD);

        RpcMessage toJsonMessage = new RpcMessage();
        toJsonMessage.setAsync(ASYNC_FIELD);
        toJsonMessage.setBody(BODY_FIELD);
        toJsonMessage.setRequest(REQUEST_FIELD);
        toJsonMessage.setHeartbeat(HEART_BEAT_FIELD);
        toJsonMessage.setId(ID_FIELD);
        String toJson = JSON.toJSONString(toJsonMessage, true);
        assertThat(fromJson).isEqualTo(toJson);
    }

    /**
     * Test get next message id.
     */
    @Test
    public void testGetNextMessageId() {
        long startMessageId = RpcMessage.getNextMessageId();
        assertThat(RpcMessage.getNextMessageId()).isEqualTo(1 + startMessageId);
        assertThat(RpcMessage.getNextMessageId()).isEqualTo(2 + startMessageId);
        assertThat(RpcMessage.getNextMessageId()).isEqualTo(3 + startMessageId);
        assertThat(RpcMessage.getNextMessageId()).isEqualTo(4 + startMessageId);
        assertThat(RpcMessage.getNextMessageId()).isEqualTo(5 + startMessageId);
    }
}
