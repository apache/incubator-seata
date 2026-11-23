/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.impl.FastjsonJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FastjsonSafeModeTest {

    private final JsonSerializer serializer = new FastjsonJsonSerializer();

    @BeforeEach
    void reset() {
        AutoTypeBean.instances = 0;
    }

    @Test
    void rejectsAutoTypeWhenGloballyEnabled() {
        withGlobalAutoType(() -> {
            assertThatThrownBy(() -> serializer.parseObject(typedJson("@type"), Object.class))
                    .isInstanceOf(JsonParseException.class)
                    .hasRootCauseMessage("safeMode not support autoType : " + AutoTypeBean.class.getName());
            assertThat(AutoTypeBean.instances).isZero();
        });
    }

    @Test
    void rejectsNestedEscapedAutoType() {
        withGlobalAutoType(() -> {
            String json = "{\"value\":" + typedJson("\\" + "u0040type") + "}";
            assertThatThrownBy(() -> serializer.parseObject(json, Map.class)).isInstanceOf(JsonParseException.class);
            assertThat(AutoTypeBean.instances).isZero();
        });
    }

    @Test
    void rejectsAutoTypeEnabledByGlobalFeatures() {
        int originalFeatures = JSON.DEFAULT_PARSER_FEATURE;
        try {
            JSON.DEFAULT_PARSER_FEATURE |= Feature.SupportAutoType.mask;
            assertThatThrownBy(() -> serializer.parseObject(typedJson("@type"), Object.class))
                    .isInstanceOf(JsonParseException.class);
            assertThat(AutoTypeBean.instances).isZero();
        } finally {
            JSON.DEFAULT_PARSER_FEATURE = originalFeatures;
        }
    }

    @Test
    void ignoresNestedAutoTypeWithoutConstructingClass() {
        withGlobalAutoType(() -> {
            String json = "{\"value\":" + typedJson("\\" + "u0040type") + "}";
            Map<?, ?> result = serializer.parseObject(json, Map.class, true);
            assertThat(result.get("value")).isInstanceOf(Map.class);
            assertThat(((Map<?, ?>) result.get("value")).get("name")).isEqualTo("test");
            assertThat(AutoTypeBean.instances).isZero();
        });
    }

    @Test
    void preservesExplicitTargetTypeAndOrdinaryJson() {
        AutoTypeBean bean = serializer.parseObject("{\"name\":\"test\"}", AutoTypeBean.class);
        assertThat(bean.name).isEqualTo("test");
        assertThat(serializer.parseObject("{\"value\":[1,2]}", Map.class).get("value"))
                .isEqualTo(java.util.Arrays.asList(1, 2));
        assertThat(serializer.parseObject(typedJson("@type"), AutoTypeBean.class, true).name)
                .isEqualTo("test");
    }

    @Test
    void preservesAllowlistedAutoTypePaths() {
        assertThat(serializer.parseObject(typedJson("@type"), Object.class, false))
                .isInstanceOf(AutoTypeBean.class);
        List<Object> values = serializer.parseObjectWithType(
                "[" + typedJson("@type") + "]", new TypeReference<List<Object>>() {}.getType());
        assertThat(values.get(0)).isInstanceOf(AutoTypeBean.class);
    }

    private static String typedJson(String typeKey) {
        return "{\"" + typeKey + "\":\"" + AutoTypeBean.class.getName() + "\",\"name\":\"test\"}";
    }

    private static void withGlobalAutoType(Runnable action) {
        ParserConfig global = ParserConfig.getGlobalInstance();
        boolean originalAutoType = global.isAutoTypeSupport();
        boolean originalSafeMode = global.isSafeMode();
        try {
            global.setSafeMode(false);
            global.setAutoTypeSupport(true);
            action.run();
            assertThat(global.isAutoTypeSupport()).isTrue();
            assertThat(global.isSafeMode()).isFalse();
        } finally {
            global.setAutoTypeSupport(originalAutoType);
            global.setSafeMode(originalSafeMode);
        }
    }

    public static class AutoTypeBean {
        static int instances;
        public String name;

        public AutoTypeBean() {
            instances++;
        }
    }
}
