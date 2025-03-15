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
package org.apache.seata.core.rpc.netty.grpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrpcHeaderEnumTest {

    @Test
    public void testHeaderValues() {
        assertEquals("grpc-status", GrpcHeaderEnum.GRPC_STATUS.header);
        assertEquals(":status", GrpcHeaderEnum.HTTP2_STATUS.header);
        assertEquals("content-type", GrpcHeaderEnum.GRPC_CONTENT_TYPE.header);
        assertEquals("codec-type", GrpcHeaderEnum.CODEC_TYPE.header);
        assertEquals("compress-type", GrpcHeaderEnum.COMPRESS_TYPE.header);
    }
}
