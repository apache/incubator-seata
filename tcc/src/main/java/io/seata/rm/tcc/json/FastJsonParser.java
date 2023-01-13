package io.seata.rm.tcc.json;

import com.alibaba.fastjson.JSON;
import io.seata.integrationapi.json.JsonParser;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public class FastJsonParser implements JsonParser {

    @Override
    public String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }
}
