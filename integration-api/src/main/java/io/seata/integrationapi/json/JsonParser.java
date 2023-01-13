package io.seata.integrationapi.json;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public interface JsonParser {

    String toJSONString(Object object);

    <T> T parseObject(String text, Class<T> clazz);

    default int order() {
        return 0;
    }

}
