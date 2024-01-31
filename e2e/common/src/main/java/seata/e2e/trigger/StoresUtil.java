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
// This file is originally from Apache SkyWalking
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
 *
 */

package seata.e2e.trigger;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author jingliu_xiong@foxmail.com
 */
public final class StoresUtil {
    public static ExtensionContext.Store store(final ExtensionContext context) {

        final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(
                context.getRequiredTestClass(),
                context.getRequiredTestMethod()
        );
        return context.getRoot().getStore(namespace);
    }

    public static <T> T get(final ExtensionContext context, final Object key, final Class<T> requiredType) {
        return store(context).get(key, requiredType);
    }

    public static void put(final ExtensionContext context, final Object key, final Object value) {
        store(context).put(key, value);
    }
}