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
package io.seata.rm.datasource.undo.parser;

import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
@JacksonStdImpl
@SuppressWarnings("serial")
public class JavaSqlClobToStringSerializer   extends StdSerializer<Object> {

        /**
         * Singleton instance to use.
         */
        public final static JavaSqlClobToStringSerializer INSTANCE = new JavaSqlClobToStringSerializer();

        /**
         *<p>
         * Note: usually you should NOT create new instances, but instead use
         * {@link #INSTANCE} which is stateless and fully thread-safe. However,
         * there are cases where constructor is needed; for example,
         * when using explicit serializer annotations like
         * {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}.
         */
        public JavaSqlClobToStringSerializer() { super(Object.class); }

        /**
         * Sometimes it may actually make sense to retain actual handled type, so...
         *
         * @since 2.5
         */
        public JavaSqlClobToStringSerializer(Class<?> handledType) {
            super(handledType, false);
        }

        @Override
        public boolean isEmpty(SerializerProvider prov, Object value) {
            return value.toString().isEmpty();
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
                throws IOException
        {
            gen.writeString(value.toString());
        }

    /* 01-Mar-2011, tatu: We were serializing as "raw" String; but generally that
     *   is not what we want, since lack of type information would imply real
     *   String type.
     */
        /**
         * Default implementation will write type prefix, call regular serialization
         * method (since assumption is that value itself does not need JSON
         * Array or Object start/end markers), and then write type suffix.
         * This should work for most cases; some sub-classes may want to
         * change this behavior.
         */
        @Override
        public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
                                      TypeSerializer typeSer)
                throws IOException
        {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                    typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, g, provider);
            typeSer.writeTypeSuffix(g, typeIdDef);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
            return createSchemaNode("string", true);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
        {
            visitStringFormat(visitor, typeHint);
        }
    }
