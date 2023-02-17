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
package io.seata.rm.tcc.json;

import com.alibaba.fastjson.JSON;

import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;
import io.seata.integration.tx.api.json.JsonParser;

/**
 * @author leezongjie
 * @author zouwei
 */
@LoadLevel(name = Constants.FASTJSON_JSON_PARSER_NAME)
public class FastJsonParser implements JsonParser {

    @Override
    public String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }

    @Override
    public String getName() {
        return Constants.FASTJSON_JSON_PARSER_NAME;
    }
}
