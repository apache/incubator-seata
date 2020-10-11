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

import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type MergedWarpMessageTest test.
 *
 * @author leizhiyuan
 */
public class MergedWarpMessageTest {

    @Test
    public void getTypeCode() {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        assertThat(mergedWarpMessage.getTypeCode()).isEqualTo(MessageType.TYPE_SEATA_MERGE);
    }

    private GlobalBeginRequest buildGlobalBeginRequest() {
        final GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);
        return globalBeginRequest;
    }
}
