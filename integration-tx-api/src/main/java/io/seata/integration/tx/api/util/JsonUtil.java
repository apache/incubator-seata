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
package io.seata.integration.tx.api.util;

import java.util.Objects;

import io.seata.common.ConfigurationKeys;
import io.seata.common.Constants;
import io.seata.common.DefaultValues;
import io.seata.config.ConfigurationFactory;
import io.seata.integration.tx.api.json.JsonParserFactory;

/**
 * @author leezongjie
 * @author zouwei
 */
public class JsonUtil {

    private static final String CONFIG_JSON_PARSER_NAME =
        ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME,
            DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER);

    public static String toJSONString(Object object) {
        return JsonParserFactory.getInstance(CONFIG_JSON_PARSER_NAME).toJSONString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (Objects.isNull(text) || Objects.isNull(clazz)) {
            return null;
        }
        String jsonParseName = text.startsWith(Constants.JACKSON_JSON_TEXT_PREFIX) ? Constants.JACKSON_JSON_PARSER_NAME
            : CONFIG_JSON_PARSER_NAME;
        return JsonParserFactory.getInstance(jsonParseName).parseObject(text, clazz);
    }
}
