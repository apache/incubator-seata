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
package org.apache.seata.serializer.hessian;

import com.caucho.hessian.io.SerializerFactory;
import org.apache.seata.core.serializer.SerializerSecurityRegistry;

/*
 * @Xin Wang
 */
public class HessianSerializerFactory extends SerializerFactory {
    public static final SerializerFactory INSTANCE = new HessianSerializerFactory();

    private HessianSerializerFactory() {
        super();
        //Serialization whitelist
        super.getClassFactory().setWhitelist(true);
        //register allow types
        registerAllowTypes();
        //register deny types
        registerDenyTypes();
    }

    public static SerializerFactory getInstance() {
        return INSTANCE;
    }

    private void registerAllowTypes() {
        for (String pattern : SerializerSecurityRegistry.getAllowClassPattern()) {
            super.getClassFactory().allow(pattern);
        }
    }

    private void registerDenyTypes() {
        for (String pattern : SerializerSecurityRegistry.getDenyClassPattern()) {
            super.getClassFactory().deny(pattern);
        }
    }
}
