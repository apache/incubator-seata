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
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Global rollback request codec test.
 *
 * @author zhangsen
 */
public class GlobalRollbackRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();
        globalRollbackRequest.setExtraData("aaaa");
        globalRollbackRequest.setXid("aaaa");

        byte[] bytes = seataCodec.encode(globalRollbackRequest);

        GlobalRollbackRequest globalRollbackRequest2 = seataCodec.decode(bytes);
        assertThat(globalRollbackRequest2.getXid()).isEqualTo(globalRollbackRequest.getXid());
        assertThat(globalRollbackRequest2.getExtraData()).isEqualTo(globalRollbackRequest.getExtraData());
    }

}
