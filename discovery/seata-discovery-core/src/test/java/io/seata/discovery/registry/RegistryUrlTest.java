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
package io.seata.discovery.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegistryUrlTest {

    @Test
    public void testValueOf() {
        String url = "seata://127.0.0.1:8080";
        RegistryUrl registryUrl = RegistryUrl.valueOf(url);
        Assertions.assertEquals(registryUrl.getHost(),"127.0.0.1");
        Assertions.assertEquals(registryUrl.getPort(),8080);
        Assertions.assertEquals(registryUrl.getSchema(), "seata");

        String url2 = "127.0.0.1";
        RegistryUrl registryUrl2 = RegistryUrl.valueOf(url2);
        Assertions.assertEquals(registryUrl2.getHost(),"127.0.0.1");
        Assertions.assertEquals(registryUrl2.getPort(),0);
        Assertions.assertEquals(registryUrl2.getSchema(), null);


        String url3 = "127.0.0.1?a&c=a";
        RegistryUrl registryUrl3 = RegistryUrl.valueOf(url3);
        Assertions.assertEquals(registryUrl3.getHost(),"127.0.0.1");
        Assertions.assertEquals(registryUrl3.getPort(),0);
        Assertions.assertEquals(registryUrl3.getStringParam("c"),"a");
    }


    @Test
    public void testParseParas() {
        HashMap<String, String> params = new HashMap<>();
        params.put("a", "b");
        params.put("b", "c");
        params.put("d", "a");
        Assertions.assertEquals(params, RegistryUrl.parseParamter(paramsMapToUrlStr(params)));

        HashMap<String, String> params1 = new HashMap<>();
        params.put("a", "b");
        Assertions.assertEquals(params1, RegistryUrl.parseParamter(paramsMapToUrlStr(params1)));
    }

    public static String paramsMapToUrlStr(HashMap<String,String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entrySet : map.entrySet()) {
            sb.append(entrySet.getKey());
            sb.append("=");
            sb.append(entrySet.getValue());
            sb.append("&");
        }
        return sb.toString();
    }

}