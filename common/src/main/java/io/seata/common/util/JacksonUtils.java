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
package io.seata.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Jackson Utils .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-17
 */
public class JacksonUtils {

    private JacksonUtils() {
    }

    /**
     * Object Mapper.
     */
    public static final ObjectMapper OM = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(JacksonUtils.class);

    /**
     * Object to Json.
     *
     * @param object object to be serialized
     * @param <T>    type of object
     * @return Json String
     */
    public static <T> String serialize2Json(T object) {
        return serialize2Json(object, false);
    }

    /**
     * Object to Json.
     *
     * @param object object to be serialized
     * @param pretty pretty print
     * @param <T>    type of object
     * @return Json String
     */
    public static <T> String serialize2Json(T object, boolean pretty) {
        try {
            if (pretty) {
                ObjectWriter objectWriter = OM.writerWithDefaultPrettyPrinter();
                return objectWriter.writeValueAsString(object);
            } else {
                return OM.writeValueAsString(object);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Object to Json failed. {}", object, e);
            throw new RuntimeException("Object to Json failed.", e);
        }
    }

    public static <T> T deserialize(String jsonStr, Class<T> type) {
        try {
            return OM.readValue(jsonStr, type);
        } catch (JsonProcessingException e) {
            LOG.error("Json to object failed. {}", type, e);
            throw new RuntimeException("Json to object failed.", e);
        }
    }

    /**
     * Json to Map.
     *
     * @param jsonStr Json String
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> deserialize2Map(String jsonStr) {
        try {
            if (StringUtils.isNotBlank(jsonStr)) {
                Map<String, Object> temp = OM.readValue(jsonStr, Map.class);
                Map<String, String> result = new HashMap<>();
                temp.forEach((key, value) -> {
                    result.put(String.valueOf(key), String.valueOf(value));
                });
                return result;
            }
            return new HashMap<>();
        } catch (JsonProcessingException e) {
            LOG.error("Json to map failed. check if the format of the json string[{}] is correct.", jsonStr, e);
            throw new RuntimeException("Json to map failed.", e);
        }
    }

    /**
     * Json to Bean.
     *
     * @param content Json String
     * @return Map
     */
    public static <T> T json2JavaBean(String content, Class<T> valueType) {
        try {
            OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OM.readValue(content, valueType);
        } catch (Exception e) {
            LOG.error("json {} to class {} failed. ", content, valueType, e);
            throw new RuntimeException("json to class failed.", e);
        }
    }
}