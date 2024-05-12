package org.apache.seata.integration.tx.api.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParserImpl implements JsonParser {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String toJSONString(Object object) throws IOException {
        return mapper.writeValueAsString(object);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) throws IOException {
        return mapper.readValue(text, clazz);
    }

    @Override
    public String getName() {
        return "customParser";
    }
}
