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
package io.seata.codec.seata.protocol.transaction;

import io.seata.codec.seata.SeataCodec;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Global status request codec test.
 *
 * @author zhangsen
 */
public class GlobalStatusRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalStatusRequest globalStatusRequest = new GlobalStatusRequest();
        globalStatusRequest.setExtraData("aaaa");
        globalStatusRequest.setXid("aaa123");

        byte[] bytes = seataCodec.encode(globalStatusRequest);

        GlobalStatusRequest globalStatusRequest2 = seataCodec.decode(bytes);
        assertThat(globalStatusRequest2.getExtraData()).isEqualTo(globalStatusRequest.getExtraData());
        assertThat(globalStatusRequest2.getXid()).isEqualTo(globalStatusRequest.getXid());
    }
}
