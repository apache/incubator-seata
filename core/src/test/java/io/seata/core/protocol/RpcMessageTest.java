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

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Rpc message test.
 *
 * @author guoyao
 */
public class RpcMessageTest {

    private static final String BODY_FIELD = "test_body";
    private static final int ID_FIELD = 100;
    private static final byte CODEC_FIELD = 1;
    private static final byte COMPRESS_FIELD = 2;
    private static final byte MSG_TYPE_FIELD = 3;
    private static final HashMap<String, String> HEAD_FIELD = new HashMap<>();

    /**
     * Test field get set from json.
     */
    @Test
    public void testFieldGetSetFromJson() {
        String fromJson = "{\n" +
                "\t\"body\":\"" + BODY_FIELD + "\",\n" +
                "\t\"codec\":" + CODEC_FIELD + ",\n" +
                "\t\"compressor\":" + COMPRESS_FIELD + ",\n" +
                "\t\"headMap\":" + HEAD_FIELD + ",\n" +
                "\t\"id\":" + ID_FIELD + ",\n" +
                "\t\"messageType\":" + MSG_TYPE_FIELD + "\n" +
                "}";
        RpcMessage fromJsonMessage = JSON.parseObject(fromJson, RpcMessage.class);
        assertThat(fromJsonMessage.getBody()).isEqualTo(BODY_FIELD);
        assertThat(fromJsonMessage.getId()).isEqualTo(ID_FIELD);

        RpcMessage toJsonMessage = new RpcMessage();
        toJsonMessage.setBody(BODY_FIELD);
        toJsonMessage.setId(ID_FIELD);
        toJsonMessage.setMessageType(MSG_TYPE_FIELD);
        toJsonMessage.setCodec(CODEC_FIELD);
        toJsonMessage.setCompressor(COMPRESS_FIELD);
        toJsonMessage.setHeadMap(HEAD_FIELD);
        String toJson = JSON.toJSONString(toJsonMessage, true);
        assertThat(fromJson).isEqualTo(toJson);
    }
}
