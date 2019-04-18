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

package io.seata.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * The type MergeResultMessage test.
 *
 * @author leizhiyuan
 */
public class MergeResultMessageTest {

    @Test
    public void getAndSetMsgs() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        final AbstractResultMessage[] msgs = new AbstractResultMessage[1];
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Success);
        msgs[0] = globalBeginResponse;
        mergeResultMessage.setMsgs(msgs);
        Assert.assertEquals(globalBeginResponse, mergeResultMessage.getMsgs()[0]);
    }

    @Test
    public void getTypeCode() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        Assert.assertEquals(AbstractMessage.TYPE_SEATA_MERGE_RESULT, mergeResultMessage.getTypeCode());
    }

    @Test
    public void encode() {
        byte[] expect = new byte[]{0, 0, 0, 17, 0, 1, 0, 2, 1, 0, 0, 3, 120, 105, 100, 0, 4, 100, 97, 116, 97};
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        final AbstractResultMessage[] msgs = new AbstractResultMessage[1];
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Success);
        msgs[0] = globalBeginResponse;
        mergeResultMessage.setMsgs(msgs);
        byte[] result = mergeResultMessage.encode();
        Assert.assertEquals(Arrays.toString(expect), Arrays.toString(result));
    }

    @Test
    public void decode() {

        byte[] result = new byte[]{0, 0, 0, 17, 0, 1, 0, 2, 1, 0, 0, 3, 120, 105, 100, 0, 4, 100, 97, 116, 97};
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Success);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        MergeResultMessage decodeResult = new MergeResultMessage();
        decodeResult.decode(buffer.writeBytes(result));
        GlobalBeginResponse decodeGlobalBeginResponse = (GlobalBeginResponse) decodeResult.getMsgs()[0];
        Assert.assertEquals(globalBeginResponse.getExtraData(), decodeGlobalBeginResponse.getExtraData());
        Assert.assertEquals(globalBeginResponse.getXid(), decodeGlobalBeginResponse.getXid());
        //msg won't be coded
        Assert.assertNull(decodeGlobalBeginResponse.getMsg());
        Assert.assertEquals(globalBeginResponse.getResultCode(), decodeGlobalBeginResponse.getResultCode());

    }

}