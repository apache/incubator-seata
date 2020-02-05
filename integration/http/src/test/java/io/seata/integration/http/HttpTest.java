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
package io.seata.integration.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.seata.core.context.RootContext;
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
import java.util.HashMap;
import java.util.Map;

import static io.seata.integration.http.AbstractHttpExecutor.convertParamOfBean;
import static io.seata.integration.http.AbstractHttpExecutor.convertParamOfJsonString;

/**
 * @author wangxb
 */
class HttpTest {

    private static final String host = "http://127.0.0.1:8081";
    private static final String getPath = "/index";
    private static final String postPath = "/testPost";
    public static final String XID = "127.0.0.1:8081:87654321";
    private static final int PARAM_TYPE_MAP = 1;
    private static final int PARAM_TYPE_BEAN = 2;

    @Test
    void testGetProviderXID() {
        RootContext.bind(XID);
        providerStart();
        consumerGetStart(PARAM_TYPE_MAP);
        RootContext.unbind();
    }

    @Test
    void testPostProviderXID() {
        RootContext.bind(XID);
        providerStart();
        consumerPostStart(PARAM_TYPE_MAP);
        RootContext.unbind();
    }

    public void providerStart() {

    }

    public static class Person {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

    }

    private void consumerPostStart(int param_type) {
        DefaultHttpExecutor httpExecuter = DefaultHttpExecutor.getInstance();
        String str = "{\n" +
                "    \"name\":\"zhangsan\",\n" +
                "    \"age\":15\n" +
                "}";
        Person person = JSON.parseObject(str, Person.class);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhangsan");
        map.put("age", 15);

        JSONObject json = new JSONObject();
        json.put("name", "zhangsan");
        json.put("age", 15);

        //The body parameter of post supports the above types (str,person,map,json)
        try {
            HttpResponse response;

            if (param_type == PARAM_TYPE_MAP) {
                response = httpExecuter.executePost(host, postPath, map, HttpResponse.class);
            } else if (param_type == PARAM_TYPE_BEAN) {
                response = httpExecuter.executePost(host, postPath, person, HttpResponse.class);
            } else {
                response = httpExecuter.executePost(host, postPath, str, HttpResponse.class);
            }

            String content = readStreamAsStr(response.getEntity().getContent());
            Assertions.assertTrue(content.contains("zhangsan") && content.contains("15"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consumerGetStart(int param_type) {
        DefaultHttpExecutor httpExecuter = DefaultHttpExecutor.getInstance();
        Map<String, String> params = new HashMap<>();
        params.put("name", "zhangsan");

        String str = "{\n" +
                "    \"name\":\"zhangsan\",\n" +
                "    \"age\":15\n" +
                "}";
        Person person = JSON.parseObject(str, Person.class);
        try {
            //support all type of parameter types
            HttpResponse response;
            if (param_type == PARAM_TYPE_MAP) {
                response = httpExecuter.executeGet(host, getPath, params, HttpResponse.class);
            } else if (param_type == PARAM_TYPE_BEAN) {
                response = httpExecuter.executeGet(host, getPath, convertParamOfBean(person), HttpResponse.class);
            } else {
                response = httpExecuter.executeGet(host, getPath, convertParamOfJsonString(str, Person.class), HttpResponse.class);
            }
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

    @Test
    void convertParamOfJsonStringTest() {

        String targetParam = "{name=zhangsan, age=15}";
        String str = "{\n" +
                "    \"name\":\"zhangsan\",\n" +
                "    \"age\":15\n" +
                "}";
        Map<String, String> map = convertParamOfJsonString(str, Person.class);
        Assertions.assertEquals(map.toString(), targetParam);
        Person person = JSON.parseObject(str, Person.class);
        map = convertParamOfBean(person);
        Assertions.assertEquals(map.toString(), targetParam);


    }
}