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
package io.seata.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        final GlobalBeginResponse globalBeginResponse = buildGlobalBeginResponse();
        msgs[0] = globalBeginResponse;
        mergeResultMessage.setMsgs(msgs);
        assertThat(globalBeginResponse).isEqualTo(mergeResultMessage.getMsgs()[0]);
    }

    @Test
    public void getTypeCode() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        assertThat(AbstractMessage.TYPE_SEATA_MERGE_RESULT).isEqualTo(mergeResultMessage.getTypeCode());
    }

    @Test
    public void encode() {
        byte[] expect = new byte[]{0, 0, 0, 17, 0, 1, 0, 2, 1, 0, 0, 3, 120, 105, 100, 0, 4, 100, 97, 116, 97};
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        final AbstractResultMessage[] msgs = new AbstractResultMessage[1];
        final GlobalBeginResponse globalBeginResponse = buildGlobalBeginResponse();
        msgs[0] = globalBeginResponse;
        mergeResultMessage.setMsgs(msgs);
        byte[] result = mergeResultMessage.encode();
        assertThat(expect).isEqualTo(result);
    }

    @Test
    public void decode() {

        byte[] result = new byte[]{0, 0, 0, 17, 0, 1, 0, 2, 1, 0, 0, 3, 120, 105, 100, 0, 4, 100, 97, 116, 97};
        final GlobalBeginResponse globalBeginResponse = buildGlobalBeginResponse();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        MergeResultMessage decodeResult = new MergeResultMessage();
        decodeResult.decode(buffer.writeBytes(result));
        GlobalBeginResponse decodeGlobalBeginResponse = (GlobalBeginResponse) decodeResult.getMsgs()[0];

        assertThat(globalBeginResponse.getExtraData()).isEqualTo(decodeGlobalBeginResponse.getExtraData());
        assertThat(globalBeginResponse.getXid()).isEqualTo(decodeGlobalBeginResponse.getXid());
        assertThat(globalBeginResponse.getResultCode()).isEqualTo(decodeGlobalBeginResponse.getResultCode());
        //msg won't be coded
        assertThat(decodeGlobalBeginResponse.getMsg()).isNull();
    }

    private GlobalBeginResponse buildGlobalBeginResponse() {
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Success);
        return globalBeginResponse;
    }

}
