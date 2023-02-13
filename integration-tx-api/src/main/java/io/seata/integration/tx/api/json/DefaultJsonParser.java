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
package io.seata.integration.tx.api.json;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author leezongjie
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
