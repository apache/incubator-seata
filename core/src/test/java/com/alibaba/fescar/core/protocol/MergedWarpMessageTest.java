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

import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * The type MergedWarpMessage test.
 *
 * @author leizhiyuan
 */
public class MergedWarpMessageTest {

    @Test
    public void encodeAndDecode() {

        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        final ArrayList<AbstractMessage> msgs = new ArrayList<>();
        final GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);
        msgs.add(globalBeginRequest);
        mergedWarpMessage.msgs = msgs;
        byte[] result = mergedWarpMessage.encode();


        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        MergedWarpMessage decodeResult = new MergedWarpMessage();
        decodeResult.decode(buffer.writeBytes(result));
        final AbstractMessage expected = decodeResult.msgs.get(0);
        Assert.assertTrue(expected instanceof GlobalBeginRequest);
        GlobalBeginRequest decodeGlobalBeginRequest = (GlobalBeginRequest) expected;
        Assert.assertEquals(globalBeginRequest.getTransactionName(), decodeGlobalBeginRequest.getTransactionName());
        Assert.assertEquals(globalBeginRequest.getTimeout(), decodeGlobalBeginRequest.getTimeout());
        Assert.assertEquals("xx", decodeGlobalBeginRequest.getTransactionName());
        Assert.assertEquals(globalBeginRequest.getTypeCode(), decodeGlobalBeginRequest.getTypeCode());

    }
}