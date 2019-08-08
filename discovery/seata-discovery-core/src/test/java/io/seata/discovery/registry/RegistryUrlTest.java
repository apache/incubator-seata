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
        System.out.println(registryUrl2.toString());
        Assertions.assertEquals(registryUrl2.getHost(),"127.0.0.1");
        Assertions.assertEquals(registryUrl2.getPort(),0);
        Assertions.assertEquals(registryUrl2.getSchema(), "");
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