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
package org.apache.seata.serializer.fastjson2;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import org.apache.seata.core.serializer.SerializerSecurityRegistry;

public class Fastjson2SerializerFactory {
    private Filter autoTypeFilter;

    private JSONReader.Feature[] jsonReaderFeature;

    private  JSONWriter.Feature[] jsonWriterFeature;
    private static final class InstanceHolder {
        public static final Fastjson2SerializerFactory INSTANCE = new Fastjson2SerializerFactory();
    }

    public Fastjson2SerializerFactory() {
        autoTypeFilter = JSONReader.autoTypeFilter(true, SerializerSecurityRegistry.getAllowClassType().toArray(new Class[]{}));

        jsonReaderFeature = new JSONReader.Feature[]{
            JSONReader.Feature.UseDefaultConstructorAsPossible,
            // If not configured, it will be serialized based on public field and getter methods by default.
            // After configuration, it will be deserialized based on non-static fields (including private).
            // It will be safer under FieldBased configuration
            JSONReader.Feature.FieldBased,
            JSONReader.Feature.IgnoreAutoTypeNotMatch,
            JSONReader.Feature.UseNativeObject
        };

        jsonWriterFeature = new JSONWriter.Feature[]{
            JSONWriter.Feature.WriteClassName,
            JSONWriter.Feature.FieldBased,
            JSONWriter.Feature.ReferenceDetection,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.NotWriteDefaultValue,
            JSONWriter.Feature.NotWriteHashMapArrayListClassName,
            JSONWriter.Feature.WriteNameAsSymbol
        };
    }

    public static Fastjson2SerializerFactory getInstance() {
        return Fastjson2SerializerFactory.InstanceHolder.INSTANCE;
    }

    public Filter getFilter() {
        return autoTypeFilter;
    }

    public JSONReader.Feature[] getJsonReaderFeatureList() {
        return jsonReaderFeature;
    }

    public JSONWriter.Feature[] getJsonWriterFeatureList() {
        return jsonWriterFeature;
    }
}
