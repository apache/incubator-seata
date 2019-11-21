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
package io.seata.saga.engine.serializer.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.seata.saga.engine.serializer.Serializer;

/**
 * Parameter serializer based on Fastjson
 *
 * @author lorne.cl
 */
public class ParamsFastjsonSerializer implements Serializer<Object, String> {

    private static final SerializerFeature[] SERIALIZER_FEATURES = new SerializerFeature[] {
        SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.WriteClassName};

    @Override
    public String serialize(Object params) {
        if (params != null) {
            return JSON.toJSONString(params, SERIALIZER_FEATURES);
        }
        return null;
    }

    @Override
    public Object deserialize(String json) {
        if (json != null) {
            return JSON.parse(json, Feature.SupportAutoType);

        }
        return null;
    }
}