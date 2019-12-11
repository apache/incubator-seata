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
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Undo Log Delete request codec test.
 *
 * @author guoyao
 * @data 2019 0704
 */
public class UndoLogDeleteRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        UndoLogDeleteRequest logDeleteRequest1 = new UndoLogDeleteRequest();
        logDeleteRequest1.setBranchType(BranchType.AT);
        logDeleteRequest1.setResourceId("t");
        logDeleteRequest1.setSaveDays((short)7);

        byte[] bytes = seataCodec.encode(logDeleteRequest1);

        UndoLogDeleteRequest logDeleteRequest2 = seataCodec.decode(bytes);

        assertThat(logDeleteRequest2.getBranchType()).isEqualTo(logDeleteRequest1.getBranchType());
        assertThat(logDeleteRequest2.getResourceId()).isEqualTo(logDeleteRequest1.getResourceId());
        assertThat(logDeleteRequest2.getSaveDays()).isEqualTo(logDeleteRequest1.getSaveDays());
    }

}