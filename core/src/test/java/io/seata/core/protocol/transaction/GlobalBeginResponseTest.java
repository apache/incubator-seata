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
package io.seata.core.protocol.transaction;

import io.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A unit test for {@link GlobalBeginResponse}
 *
 * @author liujc
 * @date 2019/3/22 11:09
 **/
public class GlobalBeginResponseTest {
    private final String XID = "test_xid";
    private final String EXTRA_DATA = "test_extra_data";
    private final ResultCode RESULT_CODE = ResultCode.Success;

    @Test
    public void testGetSetXid() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid(XID);
        Assertions.assertEquals(XID, globalBeginResponse.getXid());
    }

    @Test
    public void testGetSetExtraData() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setExtraData(EXTRA_DATA);
        Assertions.assertEquals(EXTRA_DATA, globalBeginResponse.getExtraData());
    }

    @Test
    public void testGetTypeCode() {
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        Assertions.assertEquals(GlobalBeginResponse.TYPE_GLOBAL_BEGIN_RESULT, globalBeginResponse.getTypeCode());
    }

    @Test
    public void testDoEncodeAndDecode() {
        GlobalBeginResponse globalBeginResponseOne = new GlobalBeginResponse();
        globalBeginResponseOne.setXid(XID);
        globalBeginResponseOne.setExtraData(EXTRA_DATA);
        globalBeginResponseOne.setResultCode(RESULT_CODE);
        byte[] encode = globalBeginResponseOne.encode();
        GlobalBeginResponse globalBeginResponseTwo = new GlobalBeginResponse();
        globalBeginResponseTwo.decode(ByteBuffer.wrap(encode));
        assertThat(globalBeginResponseOne.getXid()).isEqualTo(globalBeginResponseTwo.getXid());
        assertThat(globalBeginResponseOne.getExtraData()).isEqualTo(globalBeginResponseTwo.getExtraData());
        assertThat(globalBeginResponseOne.getResultCode()).isEqualTo(globalBeginResponseTwo.getResultCode());
    }
}
