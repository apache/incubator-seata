package io.seata.integrationapi.util;

import io.seata.integrationapi.json.DefaultJsonParser;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public class JsonUtil {

    public static String toJSONString(Object object) {
        return DefaultJsonParser.get().toJSONString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        return DefaultJsonParser.get().parseObject(text, clazz);
    }

}
