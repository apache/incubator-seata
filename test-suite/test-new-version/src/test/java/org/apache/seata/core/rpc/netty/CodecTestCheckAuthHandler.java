/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.server.auth.DefaultCheckAuthHandler;

/**
 * the type CodecTestCheckAuthHandler
 **/
@LoadLevel(name = "codecTestCheckAuthHandler", order = 101)
public class CodecTestCheckAuthHandler extends DefaultCheckAuthHandler {

    public static String CODEC_TEST_REG_ERROR = "codec_test_reg_error";

    @Override
    public boolean regTransactionManagerCheckAuth(RegisterTMRequest request) {
        if (CODEC_TEST_REG_ERROR.equals(request.getExtraData())) {
            return false;
        }
        return super.regTransactionManagerCheckAuth(request);
    }
}
