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
package org.apache.seata.server.cluster.raft.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.seata.common.exception.ErrorCode;
import org.apache.seata.common.exception.SeataRuntimeException;

public class CustomDeserializer extends JsonDeserializer<Class<?>> {

    String oldPackage = "io.seata.server";

    String currentPackage = "org.apache.seata.server";

    private static final List<String> PERMIT_PACKAGES = new ArrayList<>();

    static {
        PERMIT_PACKAGES.add("org.apache.seata");
        // The storage structure of vgroup is a map.
        PERMIT_PACKAGES.add("java.util.HashMap");
    }

    @Override
    public Class<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
        String className = jsonParser.getValueAsString();
        if (className.startsWith(oldPackage)) {
            className = className.replaceFirst(oldPackage, currentPackage);
        }
        for (String permitPackage : PERMIT_PACKAGES) {
            if (className.startsWith(permitPackage)) {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        throw new SeataRuntimeException(ErrorCode.ERR_DESERIALIZATION_SECURITY,
            "Failed to deserialize object: " + className + " is not permitted");
    }

}
