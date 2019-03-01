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
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by guoyao on 2019/3/1.
 */
public class RpcMessageTest {

    private final boolean ASYNC_FIELD=false;
    private final String BODY_FIELD="test_body";
    private final boolean HEART_BEAT_FIELD=true;
    private final boolean REQUEST_FIELD=false;
    private final long ID_FIELD = 100L;


    @Test
    public void testFieldGetSetFromJson() {
        String fromJson="{\n" +
                "\t\"async\":" + ASYNC_FIELD + ",\n" +
                "\t\"body\":\"" + BODY_FIELD + "\",\n" +
                "\t\"heartbeat\":" + HEART_BEAT_FIELD + ",\n" +
                "\t\"id\":" + ID_FIELD + ",\n" +
                "\t\"request\":" + REQUEST_FIELD + "\n" +
                "}";
        RpcMessage fromJsonMessage=JSON.parseObject(fromJson, RpcMessage.class);
        Assert.assertEquals(false,fromJsonMessage.isAsync());
        Assert.assertEquals(true,fromJsonMessage.isHeartbeat());
        Assert.assertEquals(false, fromJsonMessage.isRequest());
        Assert.assertEquals("test_body", fromJsonMessage.getBody());
        Assert.assertEquals(100, fromJsonMessage.getId());

        RpcMessage toJsonMessage=new RpcMessage();
        toJsonMessage.setAsync(ASYNC_FIELD);
        toJsonMessage.setBody(BODY_FIELD);
        toJsonMessage.setRequest(REQUEST_FIELD);
        toJsonMessage.setHeartbeat(HEART_BEAT_FIELD);
        toJsonMessage.setId(ID_FIELD);
        String toJson=JSON.toJSONString(toJsonMessage, true);
        Assert.assertEquals(fromJson, toJson);
    }

    @Test
    public void testGetNextMessageId() {
        long startMessageId=RpcMessage.getNextMessageId();
        Assert.assertEquals(1+startMessageId, RpcMessage.getNextMessageId());
        Assert.assertEquals(2+startMessageId, RpcMessage.getNextMessageId());
        Assert.assertEquals(3+startMessageId, RpcMessage.getNextMessageId());
        Assert.assertEquals(4+startMessageId, RpcMessage.getNextMessageId());
        Assert.assertEquals(5+startMessageId, RpcMessage.getNextMessageId());
    }
}
