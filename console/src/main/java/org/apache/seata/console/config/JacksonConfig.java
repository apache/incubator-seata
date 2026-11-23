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
package org.apache.seata.console.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    /**
     * convert long to string for return to the front end
     */
    @Bean
    public SimpleModule customModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new ValueSerializer<Long>() {
            @Override
            public void serialize(Long value, JsonGenerator jsonGenerator, SerializationContext ctxt)
                    throws JacksonException {
                if (value == null) {
                    jsonGenerator.writeString("");
                } else {
                    jsonGenerator.writeString(value.toString());
                }
            }
        });
        return module;
    }
}
