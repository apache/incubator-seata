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
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.integration.tx.api.json.JsonParserFactory;

/**
 * @author zouwei
 */
public class JsonUtil {

    public static String toJSONString(Object object) {
        return JsonParserFactory.getInstance(
            ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME,
                DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER))
            .toJSONString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (Objects.isNull(text) || Objects.isNull(clazz)) {
            return null;
        }
        String jsonParseName;
        if (text.startsWith(Constants.JACKSON_JSON_TEXT_PREFIX)) {
            jsonParseName = Constants.JACKSON_JSON_PARSER_NAME;
        } else {
            String configJsonParseName = ConfigurationFactory.getInstance().getConfig(
                ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME,
                DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER);
            jsonParseName = StringUtils.equalsIgnoreCase(configJsonParseName, Constants.JACKSON_JSON_PARSER_NAME)
                ? Constants.FASTJSON_JSON_PARSER_NAME : configJsonParseName;
        }
        return JsonParserFactory.getInstance(jsonParseName).parseObject(text, clazz);
    }
}
