package io.seata.integrationapi.json;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public class DefaultJsonParser implements JsonParser {

    protected static List<JsonParser> allJsonParsers = new ArrayList<>();

    private static class SingletonHolder {
        private static final DefaultJsonParser INSTANCE = new DefaultJsonParser();
    }

    private DefaultJsonParser() {
        initJsonParser();
    }

    public static DefaultJsonParser get() {
        return DefaultJsonParser.SingletonHolder.INSTANCE;
    }

    private void initJsonParser() {
        List<JsonParser> jsonParsers = EnhancedServiceLoader.loadAll(JsonParser.class);
        if (CollectionUtils.isNotEmpty(jsonParsers)) {
            allJsonParsers.addAll(jsonParsers);
        }
        Collections.sort(allJsonParsers, Comparator.comparingInt(JsonParser::order));
    }

    @Override
    public String toJSONString(Object object) {
        for (JsonParser jsonParser : allJsonParsers) {
            String result = jsonParser.toJSONString(object);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        for (JsonParser jsonParser : allJsonParsers) {
            T result = jsonParser.parseObject(text, clazz);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
