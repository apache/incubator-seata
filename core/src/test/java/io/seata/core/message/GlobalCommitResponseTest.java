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
package io.seata.core.message;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalCommitResponse;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Global commit response test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/24
 */
public class GlobalCommitResponseTest {

    /**
     * Test to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testToString() throws Exception {
        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();

        globalCommitResponse.setGlobalStatus(GlobalStatus.Committed);
        globalCommitResponse.setResultCode(ResultCode.Success);
        globalCommitResponse.setMsg("OK");

        System.out.println(globalCommitResponse.toString());

        Assertions.assertEquals("globalStatus=Committed,ResultCode=Success,Msg=OK", globalCommitResponse.toString());
    }

}