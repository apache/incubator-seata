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
package io.seata.integration.motan;

import com.google.common.collect.Maps;
import io.seata.core.context.RootContext;
import io.seata.integration.http.DefaultHttpExecutor;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

/**
 * @author jimin.jm@alibaba-inc.com
 * @date 2019/05/27
 */
class HttpTransactionFilterTest {

    private static final String host = "http://127.0.0.1:8081";
    private static final String path = "/index";
    private static final String XID = "127.0.0.1:8081:87654321";

    @Test
    void testGetProviderXID() {
        RootContext.bind(XID);
        providerStart();
        consumerStart();
        RootContext.unbind();
    }

    public void providerStart() {

    }

    private void consumerStart() {
        DefaultHttpExecutor httpExecuter = DefaultHttpExecutor.getInstance();
        Map<String, String> params = Maps.newHashMap();
        params.put("name", "zhangsan");
        try {
            HttpResponse response = httpExecuter.excuteGet(null, path, null, HttpResponse.class);
            String content = readStreamAsStr(response.getEntity().getContent());
            Assertions.assertEquals(content, "hello world!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readStreamAsStr(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WritableByteChannel dest = Channels.newChannel(bos);
        ReadableByteChannel src = Channels.newChannel(is);
        ByteBuffer bb = ByteBuffer.allocate(4096);

        while (src.read(bb) != -1) {
            bb.flip();
            dest.write(bb);
            bb.clear();
        }

        src.close();
        dest.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

}